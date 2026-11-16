const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const ts = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_owner_123";
const BOB_UID = "bob_other_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user cannot read or write members, attendance, or fees", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/members`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/attendance`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/fees`).get());
  await assertFails(
    unauthDb.doc(`users/${ALICE_UID}/members/m1`).set({
      userId: ALICE_UID,
      name: "Rahul",
      phone: "9876543210",
      joinDate: "2026-11-16",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
});

test("Owner can create, list, update, and delete their own member", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const memberRef = aliceDb.doc(`users/${ALICE_UID}/members/member_1`);

  await assertSucceeds(
    memberRef.set({
      userId: ALICE_UID,
      name: "Rahul Sharma",
      phone: "9876543210",
      joinDate: "2026-11-16",
      batch: "MORNING",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/members`).get());

  await assertSucceeds(
    memberRef.update({
      name: "Rahul Kumar",
      batch: "EVENING",
      updatedAt: ts(),
    })
  );

  await assertFails(
    memberRef.update({
      batch: "INVALID_BATCH",
      updatedAt: ts(),
    })
  );

  await assertSucceeds(memberRef.delete());
});

test("Shadow update with ghost field or identity spoofing is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Ghost field on create
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/members/member_ghost`).set({
      userId: ALICE_UID,
      name: "Rahul",
      phone: "9876543210",
      joinDate: "2026-11-16",
      isAdmin: true,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Identity spoofing
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/members/member_spoof`).set({
      userId: BOB_UID,
      name: "Rahul",
      phone: "9876543210",
      joinDate: "2026-11-16",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
});

test("Cross-user read and write on members, attendance, and fees are denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.doc(`users/${ALICE_UID}/members/m1`).set({
      userId: ALICE_UID,
      name: "Rahul Sharma",
      phone: "9876543210",
      joinDate: "2026-11-16",
      createdAt: ts(),
      updatedAt: ts(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/members`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/members/m1`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/members/m1`).delete());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/attendance/2026-11-16`).set({
      userId: ALICE_UID,
      date: "2026-11-16",
      records: { m1: true },
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
});

test("Owner can create and update attendance day and fee month documents", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const attRef = aliceDb.doc(`users/${ALICE_UID}/attendance/2026-11-16`);
  const feeRef = aliceDb.doc(`users/${ALICE_UID}/fees/2026-11`);

  await assertSucceeds(
    attRef.set({
      userId: ALICE_UID,
      date: "2026-11-16",
      records: { m1: true },
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    attRef.update({
      "records.m2": false,
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    feeRef.set({
      userId: ALICE_UID,
      month: "2026-11",
      records: { m1: { paid: true, paidDate: "2026-11-16" } },
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    feeRef.update({
      "records.m1": { paid: false },
      updatedAt: ts(),
    })
  );

  // Invalid date format rejected
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/attendance/invalid-date`).set({
      userId: ALICE_UID,
      date: "invalid-date",
      records: {},
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
});
