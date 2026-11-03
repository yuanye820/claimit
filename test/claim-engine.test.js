import test from "node:test";
import assert from "node:assert/strict";
import { ClaimEngine, ConflictError, ValidationError } from "../src/claim-engine.js";

test("creates an item with the full quantity remaining", () => {
  const engine = new ClaimEngine();
  const item = engine.createItem({ id: "tent", name: "Tent", total: 2 });

  assert.equal(item.id, "tent");
  assert.equal(item.total, 2);
  assert.equal(item.remaining, 2);
  assert.deepEqual(item.claims, {});
});

test("rejects an invalid total", () => {
  const engine = new ClaimEngine();
  assert.throws(
    () => engine.createItem({ id: "tent", name: "Tent", total: 0 }),
    ValidationError,
  );
  assert.throws(
    () => engine.createItem({ id: "tent", name: "Tent", total: 1.5 }),
    ValidationError,
  );
});

test("rejects a duplicate item id", () => {
  const engine = new ClaimEngine();
  engine.createItem({ id: "tent", name: "Tent", total: 1 });
  assert.throws(
    () => engine.createItem({ id: "tent", name: "Tent again", total: 1 }),
    ValidationError,
  );
});

test("rejects an unknown item", () => {
  const engine = new ClaimEngine();
  assert.throws(() => engine.getItem("missing"), ValidationError);
});

test("claim decrements remaining and records the claim", async () => {
  const engine = new ClaimEngine();
  engine.createItem({ id: "tent", name: "Tent", total: 2 });

  const item = await engine.claim("tent", "alice", 1);

  assert.equal(item.remaining, 1);
  assert.deepEqual(item.claims, { alice: 1 });
});

test("release restores the claimed quantity", async () => {
  const engine = new ClaimEngine();
  engine.createItem({ id: "tent", name: "Tent", total: 2 });
  await engine.claim("tent", "alice", 1);

  const item = await engine.release("tent", "alice", 1);

  assert.equal(item.remaining, 2);
  assert.deepEqual(item.claims, {});
});

test("a claim larger than remaining is rejected and leaves the item unchanged", async () => {
  const engine = new ClaimEngine();
  engine.createItem({ id: "tent", name: "Tent", total: 1 });

  await assert.rejects(engine.claim("tent", "alice", 2), ConflictError);
  assert.equal(engine.getItem("tent").remaining, 1);
});

test("two claims for the last unit: one succeeds, one conflicts", async () => {
  const engine = new ClaimEngine();
  engine.createItem({ id: "tent", name: "Tent", total: 1 });

  const results = await Promise.allSettled([
    engine.claim("tent", "alice", 1),
    engine.claim("tent", "bob", 1),
  ]);

  const fulfilled = results.filter((result) => result.status === "fulfilled");
  const conflicts = results.filter(
    (result) =>
      result.status === "rejected" && result.reason instanceof ConflictError,
  );

  assert.equal(fulfilled.length, 1);
  assert.equal(conflicts.length, 1);

  const item = engine.getItem("tent");
  assert.equal(item.remaining, 0);
  assert.equal(Object.values(item.claims).reduce((sum, n) => sum + n, 0), 1);
});

test("random claim/release sequences preserve the invariant", async () => {
  const engine = new ClaimEngine();
  engine.createItem({ id: "water", name: "Water", total: 5 });

  let seed = 42;
  const random = () => {
    seed = (seed * 1664525 + 1013904223) % 4294967296;
    return seed / 4294967296;
  };
  const users = ["alice", "bob", "carol"];

  for (let i = 0; i < 200; i += 1) {
    const userId = users[Math.floor(random() * users.length)];
    const quantity = 1 + Math.floor(random() * 2);

    if (random() < 0.7) {
      try {
        await engine.claim("water", userId, quantity);
      } catch (error) {
        if (!(error instanceof ConflictError)) throw error;
      }
    } else {
      try {
        await engine.release("water", userId, quantity);
      } catch (error) {
        if (!(error instanceof ValidationError)) throw error;
      }
    }

    const item = engine.getItem("water");
    const claimed = Object.values(item.claims).reduce((sum, n) => sum + n, 0);
    assert.equal(claimed + item.remaining, item.total);
    assert.ok(item.remaining >= 0);
  }
});
