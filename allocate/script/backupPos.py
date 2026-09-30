import argparse
import csv
from pymongo import MongoClient


def main():
    parser = argparse.ArgumentParser(description="Export MongoDB node positions to CSV")
    parser.add_argument("--uri", required=True, help="MongoDB connection URI")
    parser.add_argument("--output", default="nodes.csv", help="Output CSV file")
    args = parser.parse_args()

    client = MongoClient(args.uri)
    db = client["sotn"]
    collection = db["config-view-node"]

    results = []

    for doc in collection.find():
        view_node_id = doc.get("viewNodeId", "")

        # ❗过滤带括号的
        if "(" in view_node_id:
            continue

        try:
            node = doc["data"]["node"][0]
            view = node["view-topology:view"]

            name = view.get("friendly-name")
            pos_x = view.get("pos-x")
            pos_y = view.get("pos-y")

            if name is not None:
                results.append([name, pos_x, pos_y])

        except Exception as e:
            print(f"Skip doc due to error: {e}")

    # 写入 CSV
    with open(args.output, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["friendly-name", "pos-x", "pos-y"])
        writer.writerows(results)

    print(f"Exported {len(results)} records to {args.output}")


if __name__ == "__main__":
    main()
