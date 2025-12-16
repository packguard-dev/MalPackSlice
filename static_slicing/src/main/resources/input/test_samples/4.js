const crypto = require("crypto");

// Hash (sensitive)
const hash = crypto.createHash("sha256").update("password").digest("hex");

// Random bytes
const token = crypto.randomBytes(32).toString("hex");