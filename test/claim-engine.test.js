import test from "node:test";
import assert from "node:assert/strict";
import { ClaimEngine, ValidationError } from "../src/claim-engine.js";

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
