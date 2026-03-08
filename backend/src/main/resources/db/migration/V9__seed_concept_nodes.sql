-- Seed concept nodes for built-in frontend chapters.
-- This migration is idempotent and safe to run multiple times.

INSERT INTO concept_node (chapter_id, name, outline, order_no)
VALUES
  -- computer_network / tcp
  ('tcp', 'Connection Lifecycle', 'Three-way handshake and four-way teardown states.', 10),
  ('tcp', 'Reliability Mechanisms', 'Sequence number, ACK, retransmission, and timeout.', 20),
  ('tcp', 'Flow and Congestion Control', 'Sliding window, slow start, congestion avoidance.', 30),

  -- computer_network / udp
  ('udp', 'Datagram Model', 'Connectionless transport and message boundaries.', 10),
  ('udp', 'Packet Design and Checksum', 'Header fields, checksum behavior, and limits.', 20),
  ('udp', 'Typical Usage Scenarios', 'DNS, streaming, and low-latency trade-offs.', 30),

  -- computer_network / http
  ('http', 'Request Response Basics', 'Method, URI, status code, and header semantics.', 10),
  ('http', 'State and Caching', 'Cookie/session model and cache-control lifecycle.', 20),
  ('http', 'HTTP Evolution', 'HTTP/1.1 keep-alive, HTTP/2 multiplexing, HTTP/3 over QUIC.', 30),

  -- operating_system / process-thread
  ('process-thread', 'Process and Thread Model', 'Address space isolation and scheduling units.', 10),
  ('process-thread', 'Context Switching', 'Kernel/user transitions and switch overhead.', 20),
  ('process-thread', 'Synchronization', 'Mutex, semaphore, and race condition handling.', 30),

  -- operating_system / memory
  ('memory', 'Address Translation', 'Virtual memory, page table, and TLB basics.', 10),
  ('memory', 'Paging and Replacement', 'Page fault lifecycle and replacement strategies.', 20),
  ('memory', 'Allocation Strategies', 'Heap/stack behavior and fragmentation trade-offs.', 30),

  -- operating_system / io
  ('io', 'Device I/O Path', 'Interrupt, DMA, and driver interaction pipeline.', 10),
  ('io', 'File System Abstractions', 'inode, directory, and block mapping fundamentals.', 20),
  ('io', 'I/O Performance Tuning', 'Buffer cache, writeback, and sequential/random patterns.', 30)
ON CONFLICT (chapter_id, name) DO UPDATE
SET
  outline = EXCLUDED.outline,
  order_no = EXCLUDED.order_no,
  updated_at = now();

-- Prerequisite links within each chapter (later node depends on earlier node).
INSERT INTO concept_prerequisite (node_id, prereq_node_id)
SELECT n2.id, n1.id
FROM concept_node n1
JOIN concept_node n2
  ON n1.chapter_id = n2.chapter_id
 AND n1.order_no < n2.order_no
WHERE (n1.chapter_id, n1.order_no, n2.order_no) IN (
  ('tcp', 10, 20), ('tcp', 20, 30),
  ('udp', 10, 20), ('udp', 20, 30),
  ('http', 10, 20), ('http', 20, 30),
  ('process-thread', 10, 20), ('process-thread', 20, 30),
  ('memory', 10, 20), ('memory', 20, 30),
  ('io', 10, 20), ('io', 20, 30)
)
ON CONFLICT DO NOTHING;
