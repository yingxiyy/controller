import argparse
import csv
import json
from pymongo import MongoClient


def load_csv(csv_file):
    data = {"by_node_id": {}, "by_name": {}}
    with open(csv_file, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            name = row["friendly-name"]
            pos_x = int(row["pos-x"]) if row["pos-x"] else None
            pos_y = int(row["pos-y"]) if row["pos-y"] else None

            data["by_name"][name] = (pos_x, pos_y)
    return data


def load_json(json_file):
    with open(json_file, encoding="utf-8") as f:
        payload = json.load(f)

    topologies = payload.get("output", {}).get("topology", [])
    if not topologies and "topology" in payload:
        topologies = payload.get("topology", [])

    data = {"by_node_id": {}, "by_name": {}}
    for topology in topologies:
        for node in topology.get("node", []):
            view = node.get("view") or node.get("view-topology:view") or {}
            node_id = node.get("node-id")
            name = view.get("friendly-name")
            pos_x = view.get("pos-x")
            pos_y = view.get("pos-y")

            if pos_x is not None:
                pos_x = int(pos_x)
            if pos_y is not None:
                pos_y = int(pos_y)

            if node_id:
                data["by_node_id"][node_id] = (pos_x, pos_y)
            if name:
                data["by_name"][name] = (pos_x, pos_y)

    return data


def extract_site_id(view_node_id):
    if not view_node_id:
        return view_node_id
    idx = view_node_id.find("(")
    if idx == -1:
        return view_node_id
    return view_node_id[:idx]


def resolve_position(source_data, view_node_id, friendly_name):
    candidates = [
        view_node_id,
        extract_site_id(view_node_id),
    ]
    for candidate in candidates:
        if candidate and candidate in source_data["by_node_id"]:
            return source_data["by_node_id"][candidate]

    if friendly_name in source_data["by_name"]:
        return source_data["by_name"][friendly_name]

    return None


def main():
    parser = argparse.ArgumentParser(description="Update MongoDB node positions from CSV or topology JSON")
    parser.add_argument("--uri", required=True, help="MongoDB connection URI")
    parser.add_argument("--csv", help="CSV file path")
    parser.add_argument("--json", dest="json_file", help="Topology JSON file path")
    args = parser.parse_args()

    if bool(args.csv) == bool(args.json_file):
        parser.error("exactly one of --csv or --json must be provided")

    client = MongoClient(args.uri)
    db = client["sotn"]
    collection = db["config-view-node"]

    source_data = load_csv(args.csv) if args.csv else load_json(args.json_file)

    updated_count = 0

    for doc in collection.find():
        try:
            view_node_id = doc.get("viewNodeId", "")
            node = doc["data"]["node"][0]
            view = node["view-topology:view"]
            name = view.get("friendly-name")
            position = resolve_position(source_data, view_node_id, name)
            if position:
                pos_x, pos_y = position

                update_fields = {}
                if pos_x is not None:
                    update_fields["data.node.0.view-topology:view.pos-x"] = pos_x
                if pos_y is not None:
                    update_fields["data.node.0.view-topology:view.pos-y"] = pos_y

                if update_fields:
                    collection.update_one(
                        {"_id": doc["_id"]},
                        {"$set": update_fields}
                    )
                    updated_count += 1

        except Exception as e:
            print(f"Skip doc due to error: {e}")

    print(f"Updated {updated_count} records")


if __name__ == "__main__":
    main()
