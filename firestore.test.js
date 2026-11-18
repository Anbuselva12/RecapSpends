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

test("Personal expense privacy & category hiddenFromWorkspace fields work", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowTs();

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}`).set({
      userId: ALICE_UID,
      monthlyBudget: 65000,
      language: "en",
      seeded: true,
      workspaceId: "ws_family_1",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/categories/secret_cat`).set({
      userId: ALICE_UID,
      name: "Personal gifts",
      iconKey: "gift",
      colorIndex: 5,
      subcategories: ["Gifts"],
      sortOrder: 10,
      hiddenFromWorkspace: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/expenses/priv_exp`).set({
      userId: ALICE_UID,
      title: "Birthday gift",
      amount: 3200,
      dateEpochMs: Date.now(),
      categoryId: "secret_cat",
      subcategory: "Gifts",
      paymentMode: "UPI",
      isPrivate: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Workspace create, invite join, shared expense list query, kick & leave rules", async () => {
  const CHARLIE_UID = "charlie_789";
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = nowTs();

  // Invite cannot be listed publicly
  await assertSucceeds(
    aliceDb.doc("workspaceInvites/FAM12345").set({
      workspaceId: "ws_1",
      workspaceName: "Family Home",
      ownerUid: ALICE_UID,
      ownerName: "Alice",
      createdAt: ts,
    })
  );
  await assertFails(bobDb.collection("workspaceInvites").get());
  await assertSucceeds(bobDb.doc("workspaceInvites/FAM12345").get());

  // Owner creates workspace
  await assertSucceeds(
    aliceDb.doc("workspaces/ws_1").set({
      name: "Family Home",
      ownerUid: ALICE_UID,
      inviteCode: "FAM12345",
      memberIds: [ALICE_UID],
      members: {
        [ALICE_UID]: { uid: ALICE_UID, name: "Alice", role: "owner", joinedAtMs: Date.now() },
      },
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Listing all workspaces is denied
  await assertFails(aliceDb.collection("workspaces").get());
  // Non-member Bob cannot get workspace before joining
  await assertFails(bobDb.doc("workspaces/ws_1").get());

  // Charlie cannot join with a bogus invite code
  await assertFails(
    charlieDb.doc("workspaces/ws_1").update({
      memberIds: [ALICE_UID, CHARLIE_UID],
      [`members.${CHARLIE_UID}`]: {
        uid: CHARLIE_UID,
        name: "Charlie",
        role: "member",
        joinedAtMs: Date.now(),
      },
      lastJoinCode: "FAKECODE",
      updatedAt: nowTs(),
    })
  );

  // Bob joins with valid invite code FAM12345
  await assertSucceeds(
    bobDb.doc("workspaces/ws_1").update({
      memberIds: [ALICE_UID, BOB_UID],
      [`members.${BOB_UID}`]: {
        uid: BOB_UID,
        name: "Bob",
        role: "member",
        joinedAtMs: Date.now(),
      },
      lastJoinCode: "FAM12345",
      updatedAt: nowTs(),
    })
  );

  // Alice and Bob share expenses into workspace
  await assertSucceeds(
    aliceDb.doc(`workspaces/ws_1/expenses/${ALICE_UID}_e1`).set({
      workspaceId: "ws_1",
      sourceExpenseId: "e1",
      ownerUid: ALICE_UID,
      ownerName: "Alice",
      memberIds: [ALICE_UID, BOB_UID],
      title: "Milk & Groceries",
      amount: 850,
      dateEpochMs: Date.now(),
      categoryId: "groc",
      categoryName: "Groceries",
      categoryIconKey: "bag",
      categoryColorIndex: 3,
      subcategory: "Dairy",
      paymentMode: "UPI",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bobDb.doc(`workspaces/ws_1/expenses/${BOB_UID}_e2`).set({
      workspaceId: "ws_1",
      sourceExpenseId: "e2",
      ownerUid: BOB_UID,
      ownerName: "Bob",
      memberIds: [ALICE_UID, BOB_UID],
      title: "EB Bill",
      amount: 1400,
      dateEpochMs: Date.now(),
      categoryId: "util",
      categoryName: "Utilities",
      categoryIconKey: "bolt",
      categoryColorIndex: 4,
      subcategory: "Electricity",
      paymentMode: "UPI",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Listing without array-contains filter is denied, with array-contains succeeds
  await assertFails(bobDb.collection("workspaces/ws_1/expenses").get());
  await assertSucceeds(
    bobDb.collection("workspaces/ws_1/expenses").where("memberIds", "array-contains", BOB_UID).get()
  );

  // Non-member Charlie cannot read or list shared expenses belonging to Alice/Bob
  await assertFails(charlieDb.collection("workspaces/ws_1/expenses").get());
  await assertFails(
    charlieDb.collection("workspaces/ws_1/expenses").where("memberIds", "array-contains", BOB_UID).get()
  );
  const charlieSnap = await assertSucceeds(
    charlieDb.collection("workspaces/ws_1/expenses").where("memberIds", "array-contains", CHARLIE_UID).get()
  );
  if (charlieSnap.size !== 0) {
    throw new Error(`Expected 0 docs for Charlie, got ${charlieSnap.size}`);
  }
  await assertFails(charlieDb.doc(`workspaces/ws_1/expenses/${ALICE_UID}_e1`).get());

  // Bob cannot kick Alice (non-owner cannot remove others)
  await assertFails(
    bobDb.doc("workspaces/ws_1").update({
      memberIds: [BOB_UID],
      members: {
        [BOB_UID]: { uid: BOB_UID, name: "Bob", role: "member", joinedAtMs: Date.now() },
      },
      updatedAt: nowTs(),
    })
  );

  // Owner Alice kicks Bob: deletes Bob's shared expense, removes Bob from Alice's shared expense, removes Bob from workspace
  await assertSucceeds(aliceDb.doc(`workspaces/ws_1/expenses/${BOB_UID}_e2`).delete());
  await assertSucceeds(
    aliceDb.doc(`workspaces/ws_1/expenses/${ALICE_UID}_e1`).update({
      memberIds: [ALICE_UID],
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    aliceDb.doc("workspaces/ws_1").update({
      memberIds: [ALICE_UID],
      members: {
        [ALICE_UID]: { uid: ALICE_UID, name: "Alice", role: "owner", joinedAtMs: Date.now() },
      },
      updatedAt: nowTs(),
    })
  );

  // Kicked Bob can no longer read workspace or Alice's shared expense or post new ones
  await assertFails(bobDb.doc("workspaces/ws_1").get());
  await assertFails(bobDb.doc(`workspaces/ws_1/expenses/${ALICE_UID}_e1`).get());
  await assertFails(
    bobDb.doc(`workspaces/ws_1/expenses/${BOB_UID}_e3`).set({
      workspaceId: "ws_1",
      sourceExpenseId: "e3",
      ownerUid: BOB_UID,
      ownerName: "Bob",
      memberIds: [BOB_UID],
      title: "Unauthorized",
      amount: 100,
      dateEpochMs: Date.now(),
      categoryId: "groc",
      categoryName: "Groceries",
      categoryIconKey: "bag",
      categoryColorIndex: 3,
      subcategory: "",
      paymentMode: "UPI",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Charlie joins and then leaves voluntarily
  await assertSucceeds(
    charlieDb.doc("workspaces/ws_1").update({
      memberIds: [ALICE_UID, CHARLIE_UID],
      [`members.${CHARLIE_UID}`]: {
        uid: CHARLIE_UID,
        name: "Charlie",
        role: "member",
        joinedAtMs: Date.now(),
      },
      lastJoinCode: "FAM12345",
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(
    charlieDb.doc("workspaces/ws_1").update({
      memberIds: [ALICE_UID],
      members: {
        [ALICE_UID]: { uid: ALICE_UID, name: "Alice", role: "owner", joinedAtMs: Date.now() },
      },
      updatedAt: nowTs(),
    })
  );

  // Owner dissolves workspace
  await assertSucceeds(aliceDb.doc("workspaces/ws_1").delete());
  await assertSucceeds(aliceDb.doc("workspaceInvites/FAM12345").delete());
});

test("Vehicle, Bill, Project hiddenFromWorkspace and UserProfile contributedWorkspaceIds work", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowTs();

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}`).set({
      userId: ALICE_UID,
      monthlyBudget: 65000,
      language: "en",
      seeded: true,
      contributedWorkspaceIds: ["ws_1"],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/vehicles/v_hidden`).set({
      userId: ALICE_UID,
      name: "Secret Scooter",
      shortName: "Scooter",
      type: "bike",
      plate: "TN 01 AB 1234",
      odometer: 1200,
      avgMileage: 45,
      items: [],
      history: [],
      hiddenFromWorkspace: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/bills/b_hidden`).set({
      userId: ALICE_UID,
      name: "Personal Club",
      amount: 1500,
      dueEpochMs: Date.now() + 86400000,
      repeat: "Monthly",
      reminder: "3 days before",
      categoryId: "util",
      subcategory: "",
      iconKey: "bell",
      autopay: false,
      paid: false,
      hiddenFromWorkspace: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/projects/p_hidden`).set({
      userId: ALICE_UID,
      name: "Private Side Project",
      subtitle: "1 phase",
      iconKey: "house",
      colorIndex: 3,
      budget: 50000,
      phases: [],
      vendors: [],
      payments: [],
      productCatalog: [],
      hiddenFromWorkspace: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Shared vehicles, projects, bills, categories & post-departure read-only / delete-only rules", async () => {
  const CHARLIE_UID = "charlie_789";
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = nowTs();

  await assertSucceeds(
    aliceDb.doc("workspaceInvites/FAM88888").set({
      workspaceId: "ws_2",
      workspaceName: "Home Circle",
      ownerUid: ALICE_UID,
      ownerName: "Alice",
      createdAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.doc("workspaces/ws_2").set({
      name: "Home Circle",
      ownerUid: ALICE_UID,
      inviteCode: "FAM88888",
      memberIds: [ALICE_UID],
      members: { [ALICE_UID]: { uid: ALICE_UID, name: "Alice", role: "owner", joinedAtMs: Date.now() } },
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob and Charlie join
  await assertSucceeds(
    bobDb.doc("workspaces/ws_2").update({
      memberIds: [ALICE_UID, BOB_UID],
      [`members.${BOB_UID}`]: { uid: BOB_UID, name: "Bob", role: "member", joinedAtMs: Date.now() },
      lastJoinCode: "FAM88888",
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    charlieDb.doc("workspaces/ws_2").update({
      memberIds: [ALICE_UID, BOB_UID, CHARLIE_UID],
      [`members.${CHARLIE_UID}`]: { uid: CHARLIE_UID, name: "Charlie", role: "member", joinedAtMs: Date.now() },
      lastJoinCode: "FAM88888",
      updatedAt: nowTs(),
    })
  );

  // Alice shares vehicle, project, bill, category
  await assertSucceeds(
    aliceDb.doc(`workspaces/ws_2/vehicles/${ALICE_UID}_v1`).set({
      workspaceId: "ws_2",
      sourceVehicleId: "v1",
      ownerUid: ALICE_UID,
      ownerName: "Alice",
      memberIds: [ALICE_UID, BOB_UID, CHARLIE_UID],
      contributorIds: [],
      name: "Honda City",
      shortName: "City",
      type: "car",
      plate: "TN 09 CD 9999",
      odometer: 22000,
      avgMileage: 16.5,
      items: [{ id: "oil", name: "Engine oil", intervalKm: 5000 }],
      history: [],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`workspaces/ws_2/projects/${ALICE_UID}_p1`).set({
      workspaceId: "ws_2",
      sourceProjectId: "p1",
      ownerUid: ALICE_UID,
      ownerName: "Alice",
      memberIds: [ALICE_UID, BOB_UID, CHARLIE_UID],
      contributorIds: [],
      name: "House Build",
      subtitle: "2 phases",
      iconKey: "house",
      colorIndex: 2,
      budget: 1500000,
      phases: [{ name: "Foundation", budget: 500000, spent: 0 }],
      vendors: [],
      payments: [],
      productCatalog: [{ id: "bricks", name: "Bricks", unit: "count" }],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`workspaces/ws_2/bills/${ALICE_UID}_b1`).set({
      workspaceId: "ws_2",
      sourceBillId: "b1",
      ownerUid: ALICE_UID,
      ownerName: "Alice",
      memberIds: [ALICE_UID, BOB_UID, CHARLIE_UID],
      name: "ACT Fibernet",
      amount: 999,
      dueEpochMs: Date.now() + 86400000,
      repeat: "Monthly",
      reminder: "3 days before",
      categoryId: "util",
      subcategory: "Internet",
      iconKey: "bolt",
      autopay: false,
      paid: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bobDb.doc(`workspaces/ws_2/categories/${BOB_UID}_cat_pets`).set({
      workspaceId: "ws_2",
      sourceCategoryId: "cat_pets",
      ownerUid: BOB_UID,
      ownerName: "Bob",
      memberIds: [ALICE_UID, BOB_UID, CHARLIE_UID],
      name: "Pets",
      iconKey: "heart",
      colorIndex: 6,
      subcategories: ["Vet", "Treats"],
      sortOrder: 12,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Active members can query shared collections
  await assertSucceeds(
    bobDb.collection("workspaces/ws_2/vehicles").where("memberIds", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    charlieDb.collection("workspaces/ws_2/bills").where("memberIds", "array-contains", CHARLIE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("workspaces/ws_2/categories").where("memberIds", "array-contains", ALICE_UID).get()
  );

  // Bob contributes fuel expense to Alice's shared vehicle + contribution to Alice's shared project
  await assertSucceeds(
    bobDb.doc(`workspaces/ws_2/vehicles/${ALICE_UID}_v1`).update({
      contributorIds: [BOB_UID],
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    bobDb.doc(`workspaces/ws_2/expenses/${BOB_UID}_e_fuel`).set({
      workspaceId: "ws_2",
      sourceExpenseId: "e_fuel",
      ownerUid: BOB_UID,
      ownerName: "Bob",
      memberIds: [ALICE_UID, BOB_UID, CHARLIE_UID],
      title: "IndianOil Petrol",
      amount: 1050,
      dateEpochMs: Date.now(),
      categoryId: "veh",
      categoryName: "Vehicle",
      categoryIconKey: "car",
      categoryColorIndex: 1,
      subcategory: "Fuel",
      paymentMode: "UPI",
      sharedVehicleId: `${ALICE_UID}_v1`,
      sharedVehicleOwnerUid: ALICE_UID,
      odometerReading: 22250,
      fuelPricePerLitre: 105,
      fuelLitres: 10,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bobDb.doc(`workspaces/ws_2/projects/${ALICE_UID}_p1`).update({
      contributorIds: [BOB_UID],
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    bobDb.doc(`workspaces/ws_2/projectContributions/${BOB_UID}_c1`).set({
      workspaceId: "ws_2",
      sharedProjectId: `${ALICE_UID}_p1`,
      projectOwnerUid: ALICE_UID,
      ownerUid: BOB_UID,
      ownerName: "Bob",
      memberIds: [ALICE_UID, BOB_UID, CHARLIE_UID],
      dateEpochMs: Date.now(),
      title: "Bricks Load #1",
      amount: 12000,
      vendor: "Sri Murugan Bricks",
      phaseName: "Foundation",
      items: [{ productId: "bricks", name: "Bricks", unit: "count", qty: 1000, unitPrice: 12, totalPrice: 12000 }],
      linkedExpenseId: "e_proj_1",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Owner cannot overwrite/strip contributorIds when syncing vehicle
  await assertFails(
    aliceDb.doc(`workspaces/ws_2/vehicles/${ALICE_UID}_v1`).update({
      contributorIds: [],
      updatedAt: nowTs(),
    })
  );

  // Alice creates her own shared expense too (Bob will lose access to this after kick)
  await assertSucceeds(
    aliceDb.doc(`workspaces/ws_2/expenses/${ALICE_UID}_e_alice`).set({
      workspaceId: "ws_2",
      sourceExpenseId: "e_alice",
      ownerUid: ALICE_UID,
      ownerName: "Alice",
      memberIds: [ALICE_UID, BOB_UID, CHARLIE_UID],
      title: "Alice Car Wash",
      amount: 450,
      dateEpochMs: Date.now(),
      categoryId: "veh",
      categoryName: "Vehicle",
      categoryIconKey: "car",
      categoryColorIndex: 1,
      subcategory: "Service",
      paymentMode: "UPI",
      sharedVehicleId: `${ALICE_UID}_v1`,
      sharedVehicleOwnerUid: ALICE_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob is removed from ws_2; Charlie voluntarily leaves ws_2 (Charlie did NOT contribute anything)
  await assertSucceeds(
    aliceDb.doc(`workspaces/ws_2/expenses/${ALICE_UID}_e_alice`).update({
      memberIds: [ALICE_UID, CHARLIE_UID],
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    aliceDb.doc(`workspaces/ws_2/expenses/${BOB_UID}_e_fuel`).update({
      memberIds: [BOB_UID],
      retainedForDeparture: true,
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    aliceDb.doc(`workspaces/ws_2/projectContributions/${BOB_UID}_c1`).update({
      memberIds: [BOB_UID],
      retainedForDeparture: true,
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    aliceDb.doc(`workspaces/ws_2/vehicles/${ALICE_UID}_v1`).update({
      memberIds: [ALICE_UID, CHARLIE_UID],
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    aliceDb.doc(`workspaces/ws_2/projects/${ALICE_UID}_p1`).update({
      memberIds: [ALICE_UID, CHARLIE_UID],
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    aliceDb.doc(`workspaces/ws_2/bills/${ALICE_UID}_b1`).update({
      memberIds: [ALICE_UID, CHARLIE_UID],
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(aliceDb.doc(`workspaces/ws_2/categories/${BOB_UID}_cat_pets`).delete());
  await assertSucceeds(
    aliceDb.doc("workspaces/ws_2").update({
      memberIds: [ALICE_UID, CHARLIE_UID],
      members: {
        [ALICE_UID]: { uid: ALICE_UID, name: "Alice", role: "owner", joinedAtMs: Date.now() },
        [CHARLIE_UID]: { uid: CHARLIE_UID, name: "Charlie", role: "member", joinedAtMs: Date.now() },
      },
      updatedAt: nowTs(),
    })
  );

  // Charlie leaves voluntarily
  await assertSucceeds(
    charlieDb.doc(`workspaces/ws_2/expenses/${ALICE_UID}_e_alice`).update({
      memberIds: [ALICE_UID],
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    charlieDb.doc(`workspaces/ws_2/vehicles/${ALICE_UID}_v1`).update({
      memberIds: [ALICE_UID],
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    charlieDb.doc(`workspaces/ws_2/projects/${ALICE_UID}_p1`).update({
      memberIds: [ALICE_UID],
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    charlieDb.doc(`workspaces/ws_2/bills/${ALICE_UID}_b1`).update({
      memberIds: [ALICE_UID],
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    charlieDb.doc("workspaces/ws_2").update({
      memberIds: [ALICE_UID],
      members: {
        [ALICE_UID]: { uid: ALICE_UID, name: "Alice", role: "owner", joinedAtMs: Date.now() },
      },
      updatedAt: nowTs(),
    })
  );

  // Verify non-contributor Charlie lost ALL access to Alice's vehicle, project, bill, and expense
  await assertFails(charlieDb.doc(`workspaces/ws_2/vehicles/${ALICE_UID}_v1`).get());
  await assertFails(charlieDb.doc(`workspaces/ws_2/projects/${ALICE_UID}_p1`).get());
  await assertFails(charlieDb.doc(`workspaces/ws_2/bills/${ALICE_UID}_b1`).get());
  await assertFails(charlieDb.doc(`workspaces/ws_2/expenses/${ALICE_UID}_e_alice`).get());

  // Verify contributor Bob:
  // 1) CAN still read Alice's vehicle & project and ONLY his own retained expense & contribution
  await assertSucceeds(bobDb.doc(`workspaces/ws_2/vehicles/${ALICE_UID}_v1`).get());
  await assertSucceeds(
    bobDb.collection("workspaces/ws_2/vehicles").where("contributorIds", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(bobDb.doc(`workspaces/ws_2/projects/${ALICE_UID}_p1`).get());
  await assertSucceeds(
    bobDb.collection("workspaces/ws_2/projects").where("contributorIds", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(bobDb.doc(`workspaces/ws_2/expenses/${BOB_UID}_e_fuel`).get());
  await assertSucceeds(bobDb.doc(`workspaces/ws_2/projectContributions/${BOB_UID}_c1`).get());

  // 2) CANNOT read Alice's expense or Alice's bill reminder
  await assertFails(bobDb.doc(`workspaces/ws_2/expenses/${ALICE_UID}_e_alice`).get());
  await assertFails(bobDb.doc(`workspaces/ws_2/bills/${ALICE_UID}_b1`).get());

  // 3) CANNOT add new entries or edit his retained entries
  await assertFails(
    bobDb.doc(`workspaces/ws_2/expenses/${BOB_UID}_e_new`).set({
      workspaceId: "ws_2",
      sourceExpenseId: "e_new",
      ownerUid: BOB_UID,
      ownerName: "Bob",
      memberIds: [BOB_UID],
      title: "Blocked Fuel",
      amount: 500,
      dateEpochMs: Date.now(),
      categoryId: "veh",
      categoryName: "Vehicle",
      categoryIconKey: "car",
      categoryColorIndex: 1,
      subcategory: "Fuel",
      paymentMode: "UPI",
      sharedVehicleId: `${ALICE_UID}_v1`,
      sharedVehicleOwnerUid: ALICE_UID,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
  await assertFails(
    bobDb.doc(`workspaces/ws_2/expenses/${BOB_UID}_e_fuel`).update({
      amount: 9999,
      updatedAt: nowTs(),
    })
  );
  await assertFails(
    bobDb.doc(`workspaces/ws_2/projectContributions/${BOB_UID}_c1`).update({
      amount: 99999,
      updatedAt: nowTs(),
    })
  );

  // 4) CAN delete his retained entries
  await assertSucceeds(bobDb.doc(`workspaces/ws_2/expenses/${BOB_UID}_e_fuel`).delete());
  await assertSucceeds(bobDb.doc(`workspaces/ws_2/projectContributions/${BOB_UID}_c1`).delete());
});

test("Income entryType is accepted on personal and shared expenses while invalid entryType is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowTs();

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/expenses/inc_1`).set({
      userId: ALICE_UID,
      title: "Acme Salary",
      amount: 85000,
      dateEpochMs: Date.now(),
      categoryId: "inc",
      subcategory: "Salary",
      paymentMode: "Bank",
      entryType: "income",
      isPrivate: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/expenses/inc_bad`).set({
      userId: ALICE_UID,
      title: "Bad Type",
      amount: 100,
      dateEpochMs: Date.now(),
      categoryId: "inc",
      subcategory: "Salary",
      paymentMode: "Bank",
      entryType: "transfer",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc("workspaces/ws_inc").set({
      name: "Family",
      ownerUid: ALICE_UID,
      inviteCode: "INC99999",
      memberIds: [ALICE_UID],
      members: { [ALICE_UID]: { uid: ALICE_UID, name: "Alice", role: "owner", joinedAtMs: Date.now() } },
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`workspaces/ws_inc/expenses/${ALICE_UID}_inc_1`).set({
      workspaceId: "ws_inc",
      sourceExpenseId: "inc_1",
      ownerUid: ALICE_UID,
      ownerName: "Alice",
      memberIds: [ALICE_UID],
      title: "Freelance Retainer",
      amount: 25000,
      dateEpochMs: Date.now(),
      categoryId: "inc",
      categoryName: "Income",
      categoryIconKey: "rupee",
      categoryColorIndex: 4,
      subcategory: "Business / Freelance",
      paymentMode: "Bank",
      entryType: "income",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    aliceDb.doc(`workspaces/ws_inc/expenses/${ALICE_UID}_inc_bad`).set({
      workspaceId: "ws_inc",
      sourceExpenseId: "inc_bad",
      ownerUid: ALICE_UID,
      ownerName: "Alice",
      memberIds: [ALICE_UID],
      title: "Bad Shared Type",
      amount: 500,
      dateEpochMs: Date.now(),
      categoryId: "inc",
      categoryName: "Income",
      categoryIconKey: "rupee",
      categoryColorIndex: 4,
      subcategory: "Salary",
      paymentMode: "Bank",
      entryType: "bonus",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

