const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTs() {
  return new Date(Date.now() - 1000);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or list expenses or vehicles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/expenses`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/vehicles`).get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}`).get());
});

test("Authenticated user can create and read own profile, expense, vehicle, bill, category, and project", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowTs();

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}`).set({
      userId: ALICE_UID,
      monthlyBudget: 65000,
      language: "en",
      seeded: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/categories/veh`).set({
      userId: ALICE_UID,
      name: "Vehicle",
      iconKey: "car",
      colorIndex: 1,
      subcategories: ["Fuel", "Service"],
      sortOrder: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/expenses/exp_1`).set({
      userId: ALICE_UID,
      title: "Reliance Fresh",
      amount: 2846,
      dateEpochMs: Date.now(),
      categoryId: "groc",
      subcategory: "Groceries",
      paymentMode: "UPI",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/vehicles/v1`).set({
      userId: ALICE_UID,
      name: "Royal Enfield Classic 350",
      shortName: "Classic 350",
      type: "bike",
      plate: "TN 99 AB 4821",
      odometer: 18420,
      avgMileage: 34,
      items: [{ id: "oil", name: "Engine oil", intervalKm: 3000 }],
      history: [],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/bills/b1`).set({
      userId: ALICE_UID,
      name: "TNEB electricity",
      amount: 1420,
      dueEpochMs: Date.now() + 86400000,
      repeat: "Bi-monthly",
      reminder: "3 days before",
      categoryId: "util",
      subcategory: "EB bill",
      iconKey: "bolt",
      autopay: false,
      paid: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/projects/p1`).set({
      userId: ALICE_UID,
      name: "House construction",
      subtitle: "Phase 2",
      iconKey: "house",
      colorIndex: 2,
      budget: 4500000,
      phases: [{ name: "Foundation", budget: 600000, spent: 585000 }],
      vendors: [],
      payments: [],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/expenses`).get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/vehicles`).get());
});

test("Cross-user access is denied (Bob cannot read or write Alice data)", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const ts = nowTs();
    await db.doc(`users/${ALICE_UID}/expenses/exp_1`).set({
      userId: ALICE_UID,
      title: "Secret expense",
      amount: 500,
      dateEpochMs: Date.now(),
      categoryId: "groc",
      subcategory: "Groceries",
      paymentMode: "UPI",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}/expenses/exp_1`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/expenses`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/expenses/exp_2`).set({
      userId: BOB_UID,
      title: "Injected",
      amount: 100,
      dateEpochMs: Date.now(),
      categoryId: "groc",
      subcategory: "Groceries",
      paymentMode: "Cash",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});

test("Shadow update with ghost field or invalid paymentMode is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowTs();

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/expenses/bad_1`).set({
      userId: ALICE_UID,
      title: "Ghost",
      amount: 100,
      dateEpochMs: Date.now(),
      categoryId: "groc",
      subcategory: "Groceries",
      paymentMode: "UPI",
      isAdmin: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/expenses/bad_2`).set({
      userId: ALICE_UID,
      title: "Bad mode",
      amount: 100,
      dateEpochMs: Date.now(),
      categoryId: "groc",
      subcategory: "Groceries",
      paymentMode: "Crypto",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});
