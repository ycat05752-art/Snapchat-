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

test("Unauthenticated user cannot read or write any user collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("notes").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Hacker",
      username: "hacker_1",
      pnPoints: 0,
      totalSnapsSent: 0,
      darkMode: true,
      locationSharingEnabled: false,
      ghostMode: true,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can create and update user profile; cross-user read/write and ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Nilesh User",
      username: "nilesh_snap",
      bio: "Snap daily",
      email: "nilesh@example.com",
      pnPoints: 0,
      totalSnapsSent: 0,
      darkMode: true,
      locationSharingEnabled: false,
      ghostMode: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // PN Points increment update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      pnPoints: 1000,
      totalSnapsSent: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bio: "Future",
      updatedAt: new Date(Date.now() + 120000),
    })
  );

  // Reject cross-user PII read
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ bio: "Hijacked" }));
});

test("Owner notes, sent_snaps, pn_history, stories, chats, friend_states, notifications parity", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  // Note create & update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set({
      userId: ALICE_UID,
      title: "Trip Plan",
      content: "Jaipur weekend",
      colorTag: "YELLOW",
      pinned: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").update({
      title: "Jaipur Trip",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notes").get());

  // Sent snap terminal lock after EXPIRED
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sent_snaps").doc("snap_1").set({
      userId: ALICE_UID,
      recipientId: "sim_001",
      recipientName: "Rahul Kumar",
      caption: "Chai time!",
      filterName: "Golden Hour 01",
      mediaType: "photo",
      viewOnce: true,
      replayAllowed: false,
      timerSeconds: 5,
      status: "DELIVERED",
      pnAwarded: 1000,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sent_snaps").doc("snap_1").update({
      status: "EXPIRED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("sent_snaps").doc("snap_1").update({
      status: "OPENED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // PN history immutable
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("pn_history").doc("pn_1").set({
      userId: ALICE_UID,
      delta: 1000,
      reason: "Snap sent to Rahul Kumar",
      snapId: "snap_1",
      createdAt: ts,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("pn_history").doc("pn_1").update({
      delta: 9999,
    })
  );

  // Story
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("stories").doc("story_1").set({
      userId: ALICE_UID,
      authorName: "Nilesh",
      storyType: "text",
      caption: "Monsoon vibes",
      filterName: "Neon Glow 03",
      musicTrack: "Kesariya Lo-Fi",
      privacy: "friends",
      bgColorHex: "#FFFC00",
      viewCount: 0,
      expiresAtMillis: Date.now() + 86400000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Friend state & Chat message
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("friend_states").doc("sim_001").set({
      userId: ALICE_UID,
      friendId: "sim_001",
      relationStatus: "friend",
      reported: false,
      unreadCount: 0,
      lastMessage: "Hey!",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chats").doc("sim_001")
      .collection("messages").doc("msg_1").set({
        userId: ALICE_UID,
        friendId: "sim_001",
        senderRole: "user",
        messageType: "text",
        text: "Kya haal hai bhai?",
        deliveryStatus: "SENT",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Notification
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").set({
      userId: ALICE_UID,
      category: "pn_points",
      title: "+1000 PN Points!",
      body: "You earned 1000 PN Points for sending a snap.",
      readFlag: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject @DocumentId `id` field in payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_2").set({
      id: "note_2",
      userId: ALICE_UID,
      title: "Bad Note",
      content: "Has id field",
      colorTag: "BLACK",
      pinned: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});
