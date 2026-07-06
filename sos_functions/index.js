const admin = require("firebase-admin");
const serviceAccount = require("./serviceAccountKey.json"); // 🔥 استدعاء ملف المفتاح الجديد

// تفعيل الاتصال المباشر بقاعدة البيانات باستخدام مفتاح الأمان الخاص بمشروعكِ
admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();
const messaging = admin.messaging();

console.log("🎯 السيرفر المحلي لـ SafeRoute بدأ العمل ويراقب الطوارئ الآن بدون أخطاء...");

// باقي كود الاستماع كما هو بدون أي تغيير
db.collection("notifications_queue").onSnapshot((snapshot) => {
    snapshot.docChanges().forEach(async (change) => {
        if (change.type === "added") {
            const docId = change.doc.id;
            const data = change.doc.data();

            const targetToken = data.to;
            const payloadData = data.data || {};

            if (!targetToken) {
                console.log(`[${docId}] ⚠️ لم يتم العثور على توكن في هذا المستند.`);
                return;
            }

            console.log(`[${docId}] ⏳ تم رصد بلاغ استغاثة جديد.. جاري تجهيز الإشعار للتوكن: ${targetToken.substring(0, 10)}...`);

            const message = {
                token: targetToken,
                // احذفي الـ notification تماماً
                data: {
                    "title": payloadData.title || "🚨 استغاثة طارئة!",
                    "body": payloadData.body || "الرجاء المساعدة!",
                    "SOS_ALERT_ID": payloadData.SOS_ALERT_ID || "",
                    "senderName": payloadData.senderName || "مستخدم الطوارئ"
                }
                // لا تضعي android.priority هنا، الـ data كافية
            };

            try {
                const response = await messaging.send(message);
                console.log(`[${docId}] ✅ نجاح! تم إرسال الإشعار بنجاح للمستقبل. الـ Response ID: ${response}`);

                await db.collection("notifications_queue").doc(docId).delete();
                console.log(`[${docId}] 🧹 تم تنظيف الطابور وحذف المستند بنجاح.`);

            } catch (error) {
                console.error(`[${docId}] ❌ فشل إرسال الإشعار. السبب:`, error);
            }
        }
    });
});