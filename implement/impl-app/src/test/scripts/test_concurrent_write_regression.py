import unittest

from concurrent_write_regression import (
    build_update_body,
    select_distinct_och_tunnels,
    validate_restored_structure,
    validate_terminal_snapshot,
)


class ConcurrentWriteRegressionTest(unittest.TestCase):

    def test_selects_one_tunnel_per_och_on_same_node_pair(self):
        rows = [
            candidate("t1", "node-a#LINECARD-1", "node-z#LINECARD-1", "och-1"),
            candidate("t2", "node-a#LINECARD-2", "node-z#LINECARD-2", "och-1"),
            candidate("t3", "node-a#LINECARD-3", "node-z#LINECARD-3", "och-2"),
            candidate("t4", "node-a#LINECARD-4", "node-z#LINECARD-4", "och-3"),
        ]

        selected = select_distinct_och_tunnels(rows, 3)

        self.assertEqual(["t1", "t3", "t4"], [item["id"] for item in selected])

    def test_update_rpc_uses_tunnel_id_leaf_list_shape(self):
        body = build_update_body("t1", "allocate", "down")

        self.assertEqual(["t1"], body["input"]["tunnel-id"])
        self.assertTrue(body["input"]["force"])

    def test_rejects_group_without_enough_distinct_och_links(self):
        rows = [
            candidate("t1", "node-a#LINECARD-1", "node-z#LINECARD-1", "och-1"),
            candidate("t2", "node-a#LINECARD-2", "node-z#LINECARD-2", "och-1"),
        ]

        with self.assertRaisesRegex(ValueError, "distinct OCH"):
            select_distinct_och_tunnels(rows, 2)

    def test_terminal_validation_checks_every_tunnel_and_client_tp(self):
        selected = [
            candidate("t1", "node-a#LINECARD-1#PORT-C1", "node-z#LINECARD-1#PORT-C1", "och-1"),
            candidate("t2", "node-a#LINECARD-2#PORT-C2", "node-z#LINECARD-2#PORT-C2", "och-2"),
        ]
        snapshot = {
            "tunnels": {
                "t1": {"implementState": "allocate", "adminState": "down"},
                "t2": {"implementState": "allocate", "adminState": "down"},
            },
            "terminationPoints": {
                selected[0]["src"]: {"implementState": "allocate", "adminState": "down"},
                selected[0]["dst"]: {"implementState": "allocate", "adminState": "down"},
                selected[1]["src"]: {"implementState": "allocate", "adminState": "down"},
                # This is the lost update that a later full-node write can cause.
                selected[1]["dst"]: {"implementState": "implement", "adminState": "up"},
            },
            "ochLinks": {
                "och-1": {"implementState": "implement"},
                "och-2": {"implementState": "implement"},
            },
        }

        failures = validate_terminal_snapshot(selected, snapshot, "allocate", "down")

        self.assertEqual(1, len(failures))
        self.assertIn(selected[1]["dst"], failures[0])

    def test_terminal_validation_accepts_all_surviving_updates(self):
        selected = [candidate("t1", "node-a#LINECARD-1", "node-z#LINECARD-1", "och-1")]
        snapshot = {
            "tunnels": {"t1": {"implementState": "implement", "adminState": "up"}},
            "terminationPoints": {
                selected[0]["src"]: {"implementState": "implement", "adminState": "up"},
                selected[0]["dst"]: {"implementState": "implement", "adminState": "up"},
            },
            "ochLinks": {"och-1": {"implementState": "implement"}},
        }

        self.assertEqual([], validate_terminal_snapshot(selected, snapshot, "implement", "up"))

    def test_ase_implement_expects_tp_down_until_advance(self):
        selected = [candidate("t1", "node-a#LINECARD-1", "node-z#LINECARD-1", "och-1")]
        selected[0]["aseBased"] = True
        snapshot = {
            "tunnels": {"t1": {"implementState": "implement", "adminState": "up"}},
            "terminationPoints": {
                selected[0]["src"]: {"implementState": "allocate", "adminState": "down"},
                selected[0]["dst"]: {"implementState": "allocate", "adminState": "down"},
            },
            "ochLinks": {"och-1": {"implementState": "implement"}},
        }

        self.assertEqual([], validate_terminal_snapshot(
            selected, snapshot, "implement", "up", ase_aware=True))

    def test_restored_structure_detects_missing_and_duplicate_yang_keys(self):
        baseline = {
            "structure": {
                "nodeTpIds": {"node-a": ["tp-1", "tp-2"]},
                "ochSupportedTunnelRefs": {"och-1": ["t1", "t2"]},
                "tunnelRelations": {"t1": {"source": ["tp-1"], "destination": ["tp-z"],
                                             "supporting": ["och-1"]}},
                "errors": [],
            }
        }
        restored = {
            "structure": {
                "nodeTpIds": {"node-a": ["tp-1", "tp-1"]},
                "ochSupportedTunnelRefs": {"och-1": ["t1"]},
                "tunnelRelations": baseline["structure"]["tunnelRelations"],
                "errors": ["config-phy-node node-a data.node is not an array"],
            }
        }

        failures = validate_restored_structure(baseline, restored)

        self.assertTrue(any("nodeTpIds" in value for value in failures))
        self.assertTrue(any("ochSupportedTunnelRefs" in value for value in failures))
        self.assertTrue(any("not an array" in value for value in failures))

    def test_restored_structure_ignores_order_but_not_content(self):
        structure = {
            "nodeTpIds": {"node-a": ["tp-1", "tp-2"]},
            "ochSupportedTunnelRefs": {"och-1": ["t1", "t2"]},
            "tunnelRelations": {"t1": {"source": ["tp-1"], "destination": ["tp-z"],
                                         "supporting": ["och-1"]}},
            "errors": [],
        }
        restored = json_clone(structure)
        restored["nodeTpIds"]["node-a"].reverse()
        restored["ochSupportedTunnelRefs"]["och-1"].reverse()

        self.assertEqual([], validate_restored_structure(
            {"structure": structure}, {"structure": restored}))


def candidate(tunnel_id, src, dst, och):
    return {"id": tunnel_id, "src": src, "dst": dst, "och": och}


def json_clone(value):
    import json
    return json.loads(json.dumps(value))


if __name__ == "__main__":
    unittest.main()
