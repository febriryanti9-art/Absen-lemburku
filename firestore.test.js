const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_uid_001";
const EMPLOYEE_UID = "emp_uid_002";
const OUTSIDER_UID = "outsider_uid_003";
const COMPANY_CODE = "PTMAJU-882";

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

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

test("Unauthenticated users are blocked from all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ADMIN_UID).get());
  await assertFails(unauthDb.collection("companies").doc(COMPANY_CODE).get());
  await assertFails(
    unauthDb.collection("companies").doc(COMPANY_CODE).collection("overtime_entries").get()
  );
});

test("User profile & personal overtime: owner CRUD succeeds, cross-user denied", async () => {
  const empDb = testEnv.authenticatedContext(EMPLOYEE_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const ts = pastDate(5000);

  // Create profile (exact Kotlin UserProfileDoc payload without @DocumentId id)
  await assertSucceeds(
    empDb.collection("users").doc(EMPLOYEE_UID).set({
      userId: EMPLOYEE_UID,
      displayName: "Budi Santoso",
      mode: "PERSONAL",
      role: "EMPLOYEE",
      companyName: "",
      companyCode: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-user read & write rejected
  await assertFails(outsiderDb.collection("users").doc(EMPLOYEE_UID).get());
  await assertFails(outsiderDb.collection("users").get());

  // Partial profile update preserving createdAt
  await assertSucceeds(
    empDb.collection("users").doc(EMPLOYEE_UID).update({
      displayName: "Budi S.",
      mode: "GROUP",
      role: "EMPLOYEE",
      companyName: "PT Maju Bersama",
      companyCode: COMPANY_CODE,
      updatedAt: pastDate(1000),
    })
  );

  // Reject immutable createdAt tamper
  await assertFails(
    empDb.collection("users").doc(EMPLOYEE_UID).update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Personal overtime create, list, delete
  await assertSucceeds(
    empDb.collection("users").doc(EMPLOYEE_UID).collection("personal_overtime").doc("p1").set({
      userId: EMPLOYEE_UID,
      employeeName: "Budi Santoso",
      dateIso: "2026-11-19",
      startTime: "18:00",
      endTime: "20:30",
      durationMinutes: 150,
      overnight: false,
      notes: "Deploy fitur",
      status: "APPROVED",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    empDb.collection("users").doc(EMPLOYEE_UID).collection("personal_overtime").get()
  );
  await assertFails(
    outsiderDb.collection("users").doc(EMPLOYEE_UID).collection("personal_overtime").get()
  );
  await assertSucceeds(
    empDb.collection("users").doc(EMPLOYEE_UID).collection("personal_overtime").doc("p1").delete()
  );
});

test("Real-time Company flow: Admin creates company, Employee joins & submits, Admin approves/rejects", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const empDb = testEnv.authenticatedContext(EMPLOYEE_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const ts = pastDate(8000);

  // 1. Admin creates company
  await assertSucceeds(
    adminDb.collection("companies").doc(COMPANY_CODE).set({
      companyCode: COMPANY_CODE,
      companyName: "PT Maju Bersama",
      adminUid: ADMIN_UID,
      adminName: "Admin Utama",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Listing all companies is strictly forbidden
  await assertFails(adminDb.collection("companies").get());

  // 2. Admin registers as ADMIN member
  await assertSucceeds(
    adminDb.collection("companies").doc(COMPANY_CODE).collection("members").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      employeeName: "Admin Utama",
      companyCode: COMPANY_CODE,
      adminUid: ADMIN_UID,
      role: "ADMIN",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // 3. Employee looks up company by exact code
  await assertSucceeds(empDb.collection("companies").doc(COMPANY_CODE).get());

  // Employee CANNOT self-escalate to ADMIN role
  await assertFails(
    empDb.collection("companies").doc(COMPANY_CODE).collection("members").doc(EMPLOYEE_UID).set({
      userId: EMPLOYEE_UID,
      employeeName: "Hacker",
      companyCode: COMPANY_CODE,
      adminUid: ADMIN_UID,
      role: "ADMIN",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Employee joins as EMPLOYEE
  await assertSucceeds(
    empDb.collection("companies").doc(COMPANY_CODE).collection("members").doc(EMPLOYEE_UID).set({
      userId: EMPLOYEE_UID,
      employeeName: "Budi Karyawan",
      companyCode: COMPANY_CODE,
      adminUid: ADMIN_UID,
      role: "EMPLOYEE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // 4. Employee CANNOT self-approve overtime on create (must be PENDING)
  await assertFails(
    empDb.collection("companies").doc(COMPANY_CODE).collection("overtime_entries").doc("e_bad").set({
      userId: EMPLOYEE_UID,
      employeeName: "Budi Karyawan",
      companyCode: COMPANY_CODE,
      adminUid: ADMIN_UID,
      dateIso: "2026-11-19",
      startTime: "18:00",
      endTime: "21:00",
      durationMinutes: 180,
      overnight: false,
      notes: "Self approve attempt",
      status: "APPROVED",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Non-member outsider CANNOT submit overtime to company
  await assertFails(
    outsiderDb.collection("companies").doc(COMPANY_CODE).collection("overtime_entries").doc("e_out").set({
      userId: OUTSIDER_UID,
      employeeName: "Outsider",
      companyCode: COMPANY_CODE,
      adminUid: ADMIN_UID,
      dateIso: "2026-11-19",
      startTime: "18:00",
      endTime: "21:00",
      durationMinutes: 180,
      overnight: false,
      notes: "Unauthorized",
      status: "PENDING",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Employee submits valid PENDING overtime
  await assertSucceeds(
    empDb.collection("companies").doc(COMPANY_CODE).collection("overtime_entries").doc("e1").set({
      userId: EMPLOYEE_UID,
      employeeName: "Budi Karyawan",
      companyCode: COMPANY_CODE,
      adminUid: ADMIN_UID,
      dateIso: "2026-11-19",
      startTime: "22:00",
      endTime: "01:30",
      durationMinutes: 210,
      overnight: true,
      notes: "Maintenance malam",
      status: "PENDING",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // 5. Query alignment tests
  // Employee filtered query by userId succeeds; unfiltered fails
  await assertSucceeds(
    empDb.collection("companies").doc(COMPANY_CODE).collection("overtime_entries")
      .where("userId", "==", EMPLOYEE_UID).get()
  );
  await assertFails(
    empDb.collection("companies").doc(COMPANY_CODE).collection("overtime_entries").get()
  );

  // Admin filtered query by adminUid succeeds (sees all employee submissions!)
  await assertSucceeds(
    adminDb.collection("companies").doc(COMPANY_CODE).collection("overtime_entries")
      .where("adminUid", "==", ADMIN_UID).get()
  );
  await assertSucceeds(
    adminDb.collection("companies").doc(COMPANY_CODE).collection("members")
      .where("adminUid", "==", ADMIN_UID).get()
  );

  // Outsider cannot read employee's overtime
  await assertFails(
    outsiderDb.collection("companies").doc(COMPANY_CODE).collection("overtime_entries").doc("e1").get()
  );

  // 6. Employee CANNOT approve their own submission
  await assertFails(
    empDb.collection("companies").doc(COMPANY_CODE).collection("overtime_entries").doc("e1").update({
      status: "APPROVED",
      updatedAt: pastDate(1000),
    })
  );

  // 7. Admin approves employee submission
  await assertSucceeds(
    adminDb.collection("companies").doc(COMPANY_CODE).collection("overtime_entries").doc("e1").update({
      status: "APPROVED",
      updatedAt: pastDate(1000),
    })
  );

  // Employee cannot delete once APPROVED; Admin can
  await assertFails(
    empDb.collection("companies").doc(COMPANY_CODE).collection("overtime_entries").doc("e1").delete()
  );

  // Reject ghost field shadow write & future timestamp
  await assertFails(
    empDb.collection("companies").doc(COMPANY_CODE).collection("overtime_entries").doc("e_ghost").set({
      userId: EMPLOYEE_UID,
      employeeName: "Budi Karyawan",
      companyCode: COMPANY_CODE,
      adminUid: ADMIN_UID,
      dateIso: "2026-11-19",
      startTime: "18:00",
      endTime: "19:00",
      durationMinutes: 60,
      overnight: false,
      notes: "",
      status: "PENDING",
      isVerified: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    empDb.collection("companies").doc(COMPANY_CODE).collection("overtime_entries").doc("e_future").set({
      userId: EMPLOYEE_UID,
      employeeName: "Budi Karyawan",
      companyCode: COMPANY_CODE,
      adminUid: ADMIN_UID,
      dateIso: "2026-11-19",
      startTime: "18:00",
      endTime: "19:00",
      durationMinutes: 60,
      overnight: false,
      notes: "",
      status: "PENDING",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});
