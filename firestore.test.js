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
const ADMIN_UID = "admin_789";
const ADMIN_EMAIL = "mahmoudomran324@gmail.com";

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

test("Unauthenticated user: cannot create video", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("videos").doc("vid1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      videoUrl: "https://example.com/v.mp4",
      category: "community",
      isBlocked: false,
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: can create their own video", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("videos").doc("vid1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      videoUrl: "https://example.com/v.mp4",
      category: "community",
      isBlocked: false,
      createdAt: new Date(),
    })
  );
});

test("Non-author: cannot delete another user's video", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("videos").doc("vid1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      videoUrl: "https://example.com/v.mp4",
      category: "community",
      isBlocked: false,
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("videos").doc("vid1").delete());
});

test("Super Admin: can delete any video", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("videos").doc("vid1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      videoUrl: "https://example.com/v.mp4",
      category: "community",
      isBlocked: false,
      createdAt: new Date(),
    });
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL }).firestore();
  await assertSucceeds(adminDb.collection("videos").doc("vid1").delete());
});

test("Any user: can read unblocked videos", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("videos").doc("vid1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      videoUrl: "https://example.com/v.mp4",
      category: "community",
      isBlocked: false,
      createdAt: new Date(),
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(unauthDb.collection("videos").doc("vid1").get());
});
