#!/usr/bin/env python3
"""Black-box regression test for stale full-document writes in implement flows."""

import argparse
import concurrent.futures
import json
import os
import subprocess
import threading
import time
import urllib.error
import urllib.request
from datetime import datetime


ING_STATES = {"doimplementing", "deimplementing"}


def select_distinct_och_tunnels(rows, count):
    selected = []
    seen_och = set()
    for row in rows:
        och = row.get("och")
        if not och or och in seen_och:
            continue
        selected.append(row)
        seen_och.add(och)
        if len(selected) == count:
            return selected
    raise ValueError("candidate group does not contain %d distinct OCH links" % count)


def validate_terminal_snapshot(selected, snapshot, target_state, target_admin, ase_aware=False):
    failures = []
    tunnels = snapshot.get("tunnels", {})
    termination_points = snapshot.get("terminationPoints", {})
    och_links = snapshot.get("ochLinks", {})
    for item in selected:
        tunnel = tunnels.get(item["id"])
        if not tunnel:
            failures.append("missing tunnel %s" % item["id"])
        elif (tunnel.get("implementState"), tunnel.get("adminState")) != (
                target_state, target_admin):
            failures.append("tunnel %s is %s/%s, expected %s/%s" % (
                item["id"], tunnel.get("implementState"), tunnel.get("adminState"),
                target_state, target_admin))
        for tp_id in (item["src"], item["dst"]):
            tp = termination_points.get(tp_id)
            expected_tp_state = ("allocate" if ase_aware and target_state == "implement"
                                 and item.get("aseBased") else target_state)
            expected_tp_admin = ("down" if ase_aware and target_state == "implement"
                                 and item.get("aseBased") else target_admin)
            if not tp:
                failures.append("missing termination point %s" % tp_id)
            elif (tp.get("implementState"), tp.get("adminState")) != (
                    expected_tp_state, expected_tp_admin):
                failures.append("termination point %s is %s/%s, expected %s/%s" % (
                    tp_id, tp.get("implementState"), tp.get("adminState"),
                    expected_tp_state, expected_tp_admin))
        och = och_links.get(item["och"])
        if not och:
            failures.append("missing OCH link %s" % item["och"])
        elif och.get("implementState") in ING_STATES:
            failures.append("OCH link %s remains %s" % (
                item["och"], och.get("implementState")))
    return failures


def validate_restored_structure(baseline, restored):
    failures = []
    before = baseline.get("structure", {})
    after = restored.get("structure", {})
    failures.extend(after.get("errors", []))
    for label in ("nodeTpIds", "nodeXcIds", "ochSupportedTunnelRefs"):
        before_map = before.get(label, {})
        after_map = after.get(label, {})
        if set(before_map) != set(after_map):
            failures.append("%s document keys changed: before=%s after=%s" % (
                label, sorted(before_map), sorted(after_map)))
            continue
        for object_id in sorted(before_map):
            before_values = before_map.get(object_id, [])
            after_values = after_map.get(object_id, [])
            if len(after_values) != len(set(after_values)):
                failures.append("%s %s contains duplicate YANG keys: %s" % (
                    label, object_id, after_values))
            if sorted(before_values) != sorted(after_values):
                failures.append("%s %s changed: before=%s after=%s" % (
                    label, object_id, sorted(before_values), sorted(after_values)))
    before_relations = normalize_relations(before.get("tunnelRelations", {}))
    after_relations = normalize_relations(after.get("tunnelRelations", {}))
    if before_relations != after_relations:
        failures.append("tunnelRelations changed: before=%s after=%s" % (
            before_relations, after_relations))
    return failures


def normalize_relations(relations):
    return {key: {name: sorted(values) for name, values in value.items()}
            for key, value in relations.items()}


def read_properties(path):
    result = {}
    with open(path, "r", encoding="utf-8") as stream:
        for raw_line in stream:
            line = raw_line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


class MongoShell(object):
    def __init__(self, config_path, executable="mongosh"):
        properties = read_properties(config_path)
        servers = properties["mongodb.servers"].split(",")
        self.command = [
            executable, "--quiet", "--host", servers[0],
            "-u", properties["mongodb.user"], "-p", properties["mongodb.pwd"],
            "--authenticationDatabase", "admin", properties["mongodb.database"], "--eval"
        ]

    def json_eval(self, javascript):
        process = subprocess.run(
            self.command + [javascript], stdout=subprocess.PIPE, stderr=subprocess.PIPE,
            universal_newlines=True)
        if process.returncode != 0:
            raise RuntimeError(process.stderr.strip() or process.stdout.strip())
        lines = [line.strip() for line in process.stdout.splitlines() if line.strip()]
        if not lines:
            raise RuntimeError("mongosh returned no JSON")
        return json.loads(lines[-1])


def candidate_groups(mongo, minimum_count):
    # Group by both endpoint nodes. Distinct OCH links bypass same-OCH queue serialization,
    # while every worker still writes different TP entries in the same two Node documents.
    javascript = r'''
const minimum = %d;
const rows = db.getCollection("config-tunnel").aggregate([
  {$unwind:"$data.tunnel:tunnel"},
  {$replaceWith:"$data.tunnel:tunnel"},
  {$match:{"implement-state":"implement","admin-state":"up"}},
  {$project:{_id:0,id:"$tunnel-id",
    src:{$arrayElemAt:["$source-tp.tp-ref",0]},
    dst:{$arrayElemAt:["$destination-tp.tp-ref",0]},
    och:{$arrayElemAt:["$supporting-link.link-ref",0]},name:"$friendly-name"}},
  {$match:{src:{$type:"string"},dst:{$type:"string"},och:{$type:"string"}}},
  {$set:{srcNode:{$arrayElemAt:[{$split:["$src","#LINECARD"]},0]},
         dstNode:{$arrayElemAt:[{$split:["$dst","#LINECARD"]},0]}}},
  {$group:{_id:{src:"$srcNode",dst:"$dstNode"},
    ochs:{$addToSet:"$och"},items:{$push:{id:"$id",src:"$src",dst:"$dst",och:"$och",name:"$name"}}}},
  {$set:{ochCount:{$size:"$ochs"}}},{$match:{ochCount:{$gte:minimum}}},
  {$sort:{ochCount:-1}},{$limit:50}
]).toArray();
const aseCache = {};
function isAseBased(ochId) {
  if (Object.prototype.hasOwnProperty.call(aseCache, ochId)) return aseCache[ochId];
  const ochDoc = db.getCollection("config-och-link").findOne({ochLinkId:ochId});
  const ochLink = ochDoc && ochDoc.data && Array.isArray(ochDoc.data.link)
    ? ochDoc.data.link[0] : null;
  const siteLinkIds = ochLink && Array.isArray(ochLink["supporting-link"])
    ? ochLink["supporting-link"].map(v=>v["link-ref"])
        .filter(v=>typeof v === "string" && v.startsWith("SiteLink-")) : [];
  // Match LinkImplementState.aseBasedOchLink: every supporting SiteLink has dummy-link.
  aseCache[ochId] = siteLinkIds.length > 0 && siteLinkIds.every(siteLinkId => {
    const siteDoc = db.getCollection("config-site-link").findOne({siteLinkId:siteLinkId});
    const siteLink = siteDoc && siteDoc.data && Array.isArray(siteDoc.data.link)
      ? siteDoc.data.link[0] : null;
    const site = siteLink ? siteLink["site-topology:site"] : null;
    return site && site["dummy-link"] != null;
  });
  return aseCache[ochId];
}
rows.forEach(row => row.items.forEach(item => item.aseBased = isAseBased(item.och)));
print(JSON.stringify(rows));
''' % minimum_count
    return mongo.json_eval(javascript)


def topology_snapshot(mongo, selected):
    tunnel_ids = [item["id"] for item in selected]
    tp_ids = [tp for item in selected for tp in (item["src"], item["dst"])]
    och_ids = [item["och"] for item in selected]
    node_ids = sorted(set(tp.split("#LINECARD", 1)[0] for tp in tp_ids))
    javascript = r'''
const tunnelIds = %s;
const tpIds = %s;
const ochIds = %s;
const nodeIds = %s;
const result = {tunnels:{},terminationPoints:{},ochLinks:{},structure:{
  nodeTpIds:{},nodeXcIds:{},ochSupportedTunnelRefs:{},tunnelRelations:{},errors:[]}};
db.getCollection("config-tunnel").find({tunnelId:{$in:tunnelIds}}).forEach(d => {
  if (!d.data || !Array.isArray(d.data["tunnel:tunnel"])) {
    result.structure.errors.push("config-tunnel "+d.tunnelId+" data.tunnel:tunnel is not an array"); return;
  }
  const t=d.data["tunnel:tunnel"][0];
  result.tunnels[d.tunnelId]={implementState:t["implement-state"],adminState:t["admin-state"]};
  result.structure.tunnelRelations[d.tunnelId]={
    source:(t["source-tp"]||[]).map(v=>v["tp-ref"]),
    destination:(t["destination-tp"]||[]).map(v=>v["tp-ref"]),
    supporting:(t["supporting-link"]||[]).map(v=>v["link-ref"])};
});
db.getCollection("config-phy-node").find({neId:{$in:nodeIds}}).forEach(d => {
  if (!d.data || !Array.isArray(d.data.node)) {
    result.structure.errors.push("config-phy-node "+d.neId+" data.node is not an array"); return;
  }
  const node=d.data.node[0]||{};
  if (!Array.isArray(node["termination-point"])) {
    result.structure.errors.push("config-phy-node "+d.neId+" termination-point is not an array");
  }
  const tps=Array.isArray(node["termination-point"])?node["termination-point"]:[];
  result.structure.nodeTpIds[d.neId]=tps.map(tp=>tp["tp-id"]);
  const physical=node["otn-phy-topology:physical"]||{};
  const xcs=Array.isArray(physical["cross-connections"])?physical["cross-connections"]:[];
  result.structure.nodeXcIds[d.neId]=xcs.map(xc=>xc["cross-connection-id"]);
  tps.forEach(tp => {
    if (!tpIds.includes(tp["tp-id"])) return;
    const p=tp["otn-phy-topology:physical"]||{};
    result.terminationPoints[tp["tp-id"]]={implementState:p["implement-state"],adminState:p["admin-state"]};
  });
});
db.getCollection("config-och-link").find({ochLinkId:{$in:ochIds}}).forEach(d => {
  if (!d.data || !Array.isArray(d.data.link)) {
    result.structure.errors.push("config-och-link "+d.ochLinkId+" data.link is not an array"); return;
  }
  const link=d.data.link[0]||{};
  const och=link["och-topology:och"]||{};
  const supported=Array.isArray(link["tunnel:supported-tunnel"])?link["tunnel:supported-tunnel"]:[];
  result.ochLinks[d.ochLinkId]={implementState:och["implement-state"],adminState:och["admin-state"],
    supportedTunnelCount:supported.length};
  result.structure.ochSupportedTunnelRefs[d.ochLinkId]=supported.map(v=>v["tunnel-ref"]);
});
print(JSON.stringify(result));
''' % tuple(json.dumps(value, ensure_ascii=False) for value in (
        tunnel_ids, tp_ids, och_ids, node_ids))
    return mongo.json_eval(javascript)


def post_tunnel_state(base_url, item, target_state, target_admin, barrier, timeout):
    body = build_update_body(item["id"], target_state, target_admin)
    request = urllib.request.Request(
        base_url.rstrip("/") + "/restconf/operations/tunnel:update-tunnel-sync",
        data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json"}, method="POST")
    barrier.wait()
    started = time.time()
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            payload = response.read().decode("utf-8", "replace")
            return {"id": item["id"], "started": started, "ended": time.time(),
                    "httpCode": response.status, "body": parse_json_or_text(payload)}
    except urllib.error.HTTPError as error:
        payload = error.read().decode("utf-8", "replace")
        return {"id": item["id"], "started": started, "ended": time.time(),
                "httpCode": error.code, "body": parse_json_or_text(payload)}
    except Exception as error:
        return {"id": item["id"], "started": started, "ended": time.time(),
                "httpCode": 0, "body": str(error)}


def parse_json_or_text(value):
    try:
        return json.loads(value)
    except ValueError:
        return value


def build_update_body(tunnel_id, target_state, target_admin):
    body = {"input": {
        # update-tunnel-sync models tunnel-id as a YANG leaf-list, including one-ID calls.
        "tunnel-id": [tunnel_id],
        "admin-state": target_admin,
        "implement-state": target_state,
    }}
    if target_state == "allocate":
        body["input"]["force"] = True
    return body


def run_phase(mongo, selected, base_url, target_state, target_admin, timeout, poll_interval,
              settle_seconds=15, required_concurrency=2, ase_aware=False):
    barrier = threading.Barrier(len(selected))
    with concurrent.futures.ThreadPoolExecutor(max_workers=len(selected)) as executor:
        futures = [executor.submit(post_tunnel_state, base_url, item, target_state,
                                   target_admin, barrier, 30) for item in selected]
        responses = [future.result() for future in futures]

    timeline = []
    max_active = 0
    deadline = time.time() + timeout
    final_snapshot = None
    settled_since = None
    failures = ["HTTP request failed for %s: %s" % (item["id"], item["httpCode"])
                for item in responses if not 200 <= item["httpCode"] < 300]
    while time.time() < deadline:
        snapshot = topology_snapshot(mongo, selected)
        active = sum(1 for state in snapshot.get("tunnels", {}).values()
                     if state.get("implementState") in ING_STATES)
        max_active = max(max_active, active)
        timeline.append({"time": datetime.now().isoformat(), "active": active,
                         "tunnels": snapshot.get("tunnels", {})})
        terminal_failures = validate_terminal_snapshot(
            selected, snapshot, target_state, target_admin, ase_aware)
        if not terminal_failures:
            # DB terminal state is written before the lifecycle worker exits. Require a stable
            # interval so the next phase cannot overlap the tail of the previous operation.
            settled_since = settled_since or time.time()
            if time.time() - settled_since >= settle_seconds:
                final_snapshot = snapshot
                break
        else:
            settled_since = None
        states = [value.get("implementState")
                  for value in snapshot.get("tunnels", {}).values()]
        if active == 0 and "partial-implement" in states:
            failures.extend(terminal_failures)
            failures.append("phase stopped after a tunnel reached partial-implement")
            final_snapshot = snapshot
            break
        time.sleep(poll_interval)
    if final_snapshot is None:
        final_snapshot = topology_snapshot(mongo, selected)
        failures.extend(validate_terminal_snapshot(
            selected, final_snapshot, target_state, target_admin, ase_aware))
    # A PASS requires evidence that stale ChangedObjects could coexist.
    if max_active < required_concurrency:
        failures.append("concurrency window not observed: max simultaneous ing=%d" % max_active)
    return {"targetState": target_state, "targetAdmin": target_admin,
            "responses": responses, "maxSimultaneousIng": max_active,
            "timeline": timeline, "finalSnapshot": final_snapshot, "failures": failures}


def restore_tp_states(mongo, selected, baseline):
    states = {}
    baseline_tps = baseline.get("terminationPoints", {})
    for item in selected:
        for tp_id in (item["src"], item["dst"]):
            if tp_id in baseline_tps:
                states[tp_id] = baseline_tps[tp_id]
    javascript = r'''
const states = %s;
const result = [];
Object.keys(states).forEach(tpId => {
  const neId = tpId.split("#LINECARD", 1)[0];
  const state = states[tpId];
  const update = {$set:{}};
  update.$set["data.node.0.termination-point.$[tp].otn-phy-topology:physical.admin-state"] = state.adminState;
  update.$set["data.node.0.termination-point.$[tp].otn-phy-topology:physical.implement-state"] = state.implementState;
  const changed = db.getCollection("config-phy-node").updateOne(
    {neId:neId}, update, {arrayFilters:[{"tp.tp-id":tpId}]});
  result.push({tpId:tpId,matched:changed.matchedCount,modified:changed.modifiedCount});
});
print(JSON.stringify(result));
''' % json.dumps(states, ensure_ascii=False)
    return mongo.json_eval(javascript)


def find_valid_selection(mongo, count):
    reasons = []
    for group in candidate_groups(mongo, count):
        try:
            selected = select_distinct_och_tunnels(group.get("items", []), count)
        except ValueError as error:
            reasons.append(str(error))
            continue
        baseline = topology_snapshot(mongo, selected)
        failures = validate_terminal_snapshot(selected, baseline, "implement", "up")
        if not failures:
            return selected, baseline
        reasons.extend(failures)
    raise RuntimeError("no valid candidate group: %s" % "; ".join(reasons[-10:]))


def write_json(path, value):
    with open(path, "w", encoding="utf-8") as stream:
        json.dump(value, stream, ensure_ascii=False, indent=2, sort_keys=True)
        stream.write("\n")


def capture_log_evidence(log_path, offset, selected, output_path):
    if not log_path or not os.path.exists(log_path):
        return
    ids = [item["id"] for item in selected]
    with open(log_path, "rb") as source:
        source.seek(offset)
        text = source.read().decode("utf-8", "replace")
    with open(output_path, "w", encoding="utf-8") as target:
        for line in text.splitlines():
            if any(tunnel_id in line for tunnel_id in ids):
                target.write(line + "\n")


def main():
    parser = argparse.ArgumentParser(
        description="Exercise concurrent tunnel writes against shared config-phy-node documents")
    parser.add_argument("--mongo-config", default=(
        "/home/dci/deploy/impl-app-1.0.0-SNAPSHOT/config/mongodb.properties"))
    parser.add_argument("--implement-base", default="http://127.0.0.1:18007")
    parser.add_argument("--implement-log", default=(
        "/home/dci/deploy/impl-app-1.0.0-SNAPSHOT/log/implement.log"))
    parser.add_argument("--count", type=int, default=4)
    parser.add_argument("--rounds", type=int, default=2)
    parser.add_argument("--timeout", type=int, default=900)
    parser.add_argument("--poll-interval", type=float, default=0.5)
    parser.add_argument("--settle-seconds", type=float, default=15)
    parser.add_argument("--output-root", default="/tmp/dciworld/data/testcase")
    args = parser.parse_args()

    run_id = "concurrent-write-%s" % time.strftime("%Y%m%d-%H%M%S")
    output_dir = os.path.join(args.output_root, run_id)
    os.makedirs(output_dir, exist_ok=True)
    mongo = MongoShell(args.mongo_config)
    selected, baseline = find_valid_selection(mongo, args.count)
    log_offset = os.path.getsize(args.implement_log) if os.path.exists(args.implement_log) else 0
    result = {"runId": run_id, "selected": selected, "baseline": baseline,
              "rounds": [], "verdict": "PASS", "failures": []}
    # Persist the exact pre-operation structure even if the process is interrupted later.
    write_json(os.path.join(output_dir, "result.json"), result)

    try:
        for round_number in range(1, args.rounds + 1):
            round_result = {"round": round_number, "phases": []}
            # Deimplementation of tunnels on the same optical route is intentionally serialized
            # by the shared SiteLink lock. Concurrency is required in the implementation phase.
            allocate = run_phase(mongo, selected, args.implement_base, "allocate", "down",
                                 args.timeout, args.poll_interval, args.settle_seconds,
                                 required_concurrency=1)
            round_result["phases"].append(allocate)
            if allocate["failures"]:
                result["failures"].extend(
                    ["round %d allocate: %s" % (round_number, value)
                     for value in allocate["failures"]])
            implement = run_phase(mongo, selected, args.implement_base, "implement", "up",
                                  args.timeout, args.poll_interval, args.settle_seconds,
                                  ase_aware=True)
            round_result["phases"].append(implement)
            if implement["failures"]:
                result["failures"].extend(
                    ["round %d implement: %s" % (round_number, value)
                     for value in implement["failures"]])
            result["rounds"].append(round_result)
            write_json(os.path.join(output_dir, "result.json"), result)
    finally:
        current = topology_snapshot(mongo, selected)
        if validate_terminal_snapshot(selected, current, "implement", "up", True):
            result["restore"] = run_phase(
                mongo, selected, args.implement_base, "implement", "up",
                args.timeout, args.poll_interval, args.settle_seconds, ase_aware=True)
        # A no-IP lab cannot run advance adjustment. Restore only the TP state leaves captured
        # in the baseline; all topology arrays and relationships remain under test observation.
        result["tpStateRestore"] = restore_tp_states(mongo, selected, baseline)
        restore_failures = ["TP restore did not match %s" % value["tpId"]
                            for value in result["tpStateRestore"]
                            if value.get("matched") != 1]
        result["failures"].extend(restore_failures)
        result["finalSnapshot"] = topology_snapshot(mongo, selected)
        final_state_failures = validate_terminal_snapshot(
            selected, result["finalSnapshot"], "implement", "up")
        result["finalStateFailures"] = final_state_failures
        result["failures"].extend(
            ["restored state: %s" % value for value in final_state_failures])
        structure_failures = validate_restored_structure(
            baseline, result["finalSnapshot"])
        result["structureFailures"] = structure_failures
        result["failures"].extend(
            ["restored structure: %s" % value for value in structure_failures])
        if result["failures"]:
            result["verdict"] = ("INCONCLUSIVE" if all(
                "concurrency window not observed" in value for value in result["failures"])
                else "FAIL")
        write_json(os.path.join(output_dir, "result.json"), result)
        capture_log_evidence(args.implement_log, log_offset, selected,
                             os.path.join(output_dir, "implement-evidence.log"))

    print(json.dumps({"verdict": result["verdict"], "output": output_dir,
                      "selected": [item["id"] for item in selected],
                      "failures": result["failures"]}, ensure_ascii=False, indent=2))
    return 0 if result["verdict"] == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
