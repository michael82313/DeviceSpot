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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user: cannot read Alice's devices", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("devices").get());
});

test("Authenticated user: Alice can create and read her own device", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("devices").doc("dev1").set({
      userId: ALICE_UID,
      qrCode: "QR-TEST-001",
      name: "Oscilloscope Pro",
      category: "Testing & Lab Tools",
      storageLocation: "Lab Shelf 1A",
      currentLocation: "Lab Shelf 1A",
      status: "In Storage",
      createdAt: now,
    })
  );

  const doc = await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("devices").doc("dev1").get()
  );
  if (!doc.exists) {
    throw new Error("Expected doc to exist");
  }
});

test("Authenticated user: Bob cannot read Alice's device", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("devices").doc("dev1").set({
      userId: ALICE_UID,
      qrCode: "QR-TEST-001",
      name: "Oscilloscope Pro",
      category: "Testing & Lab Tools",
      storageLocation: "Lab Shelf 1A",
      currentLocation: "Lab Shelf 1A",
      status: "In Storage",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("devices").doc("dev1").get());
});

test("Authenticated user: Bob cannot create a device under Alice's path", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("devices").doc("dev2").set({
      userId: BOB_UID,
      qrCode: "QR-BOB-001",
      name: "Bob's Device",
      category: "Mobile",
      storageLocation: "Office",
      currentLocation: "Office",
      status: "In Storage",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: Alice can create location log, but cannot update it (immutable)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("device_location_logs").doc("log1").set({
      userId: ALICE_UID,
      deviceId: "dev1",
      deviceQrCode: "QR-TEST-001",
      deviceName: "Oscilloscope Pro",
      fromLocation: "None",
      toLocation: "Lab Shelf 1A",
      action: "Registered Device",
      createdAt: now,
    })
  );

  // Update should fail because logs are immutable
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("device_location_logs").doc("log1").update({
      note: "Tampered note",
    })
  );
});
