/**
 * ClaimIt domain model.
 *
 * This module is the shared contract for the Android and web clients.
 * It is deliberately framework-free so the claim rules can be tested
 * without a device, a browser or a Firebase project.
 */

export class ValidationError extends Error {
  constructor(message) {
    super(message);
    this.name = "ValidationError";
  }
}

export class ConflictError extends Error {
  constructor(message) {
    super(message);
    this.name = "ConflictError";
  }
}

export class ClaimEngine {
  #items = new Map();

  /**
   * Create a list item with an integer total quantity.
   * @param {{ id: string, name: string, total: number }} input
   */
  createItem({ id, name, total }) {
    if (typeof id !== "string" || id.trim() === "") {
      throw new ValidationError("id is required");
    }
    if (typeof name !== "string" || name.trim() === "") {
      throw new ValidationError("name is required");
    }
    if (!Number.isInteger(total) || total <= 0) {
      throw new ValidationError("total must be a positive integer");
    }
    if (this.#items.has(id)) {
      throw new ValidationError(`item ${id} already exists`);
    }

    this.#items.set(id, {
      id,
      name,
      total,
      remaining: total,
      claims: new Map(),
    });
    return this.#snapshot(this.#items.get(id));
  }

  /**
   * Claim a quantity for a user.
   * The trial change in issue #3 implements this method.
   *
   * @param {string} itemId
   * @param {string} userId
   * @param {number} quantity
   */
  async claim(itemId, userId, quantity = 1) {
    throw new Error("Not implemented: see issue #3");
  }

  /**
   * Release a quantity previously claimed by a user.
   *
   * @param {string} itemId
   * @param {string} userId
   * @param {number} quantity
   */
  async release(itemId, userId, quantity = 1) {
    throw new Error("Not implemented: see issue #3");
  }

  getItem(itemId) {
    return this.#snapshot(this.#get(itemId));
  }

  #get(itemId) {
    const item = this.#items.get(itemId);
    if (!item) {
      throw new ValidationError(`item ${itemId} does not exist`);
    }
    return item;
  }

  #snapshot(item) {
    return {
      id: item.id,
      name: item.name,
      total: item.total,
      remaining: item.remaining,
      claims: Object.fromEntries(item.claims),
    };
  }
}
