import random

def generate_initial_graph(num_nodes, num_edges):
    edges = set()
    while len(edges) < num_edges:
        if len(edges) % 100_000 == 0:
            print(f"Generated {len(edges)} edges")
        u = random.randint(1, num_nodes)
        v = random.randint(1, num_nodes)
        if u != v:
            edges.add((u, v))
    return list(edges)

def generate_batch_operations(num_operations, write_percentage, node_id_range):
    operations = []
    for _ in range(num_operations):
        op_type = random.choices(
            ['Q', 'A', 'D'],
            weights=[100 - write_percentage, write_percentage // 2, write_percentage // 2]
        )[0]
        u = random.randint(1, node_id_range)
        v = random.randint(1, node_id_range)
        while u == v:
            v = random.randint(1, node_id_range)
        operations.append((op_type, u, v))
    return operations

def write_input_file(filename, batches):
    with open(filename, 'w') as f:
        # Write batches
        for batch in batches:
            for op in batch:
                f.write(f"{op[0]} {op[1]} {op[2]}\n")
            f.write("F\n")

def write_initial_graph(filename, edges):
    with open(filename, 'w') as f:
        for u, v in edges:
            f.write(f"{u} {v}\n")
        f.write("S\n")

def main():
    # Configuration
    # Total number of nodes in the graph
    num_nodes = 1_000

    # Initial number of edges
    num_edges = 500_000

    # Number of batches
    num_batches = 1000

    # Operations per batch
    max_ops_per_batch = 10

    # % of operations that are writes (A or D)
    write_percentage = 50

    graph_filename = "resources/initial_graph.txt"
    output_filename = "resources/input"

    # Generate initial graph
    edges = generate_initial_graph(num_nodes, num_edges)

    # Generate batches
    batches = []
    for i in range(num_batches):
        if i % 100 == 0:
            print(f"Generating batch {i} of {num_batches}")

        batch = generate_batch_operations(max_ops_per_batch, write_percentage, num_nodes)
        batches.append(batch)

    # Write all to file
    write_initial_graph(graph_filename, edges)
    write_input_file(output_filename, batches)
    print(f"Multi-batch input file '{output_filename}' generated with {num_batches} batches.")

if __name__ == "__main__":
    main()
