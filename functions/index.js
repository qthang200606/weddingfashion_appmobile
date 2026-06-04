const express = require("express");
const admin = require("firebase-admin");
const bodyParser = require("body-parser");

// Khởi tạo Firebase Admin (Bạn cần file serviceAccountKey.json từ Firebase)
const serviceAccount = require("./serviceAccountKey.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const app = express();
app.use(bodyParser.json());

// 1. Route kiểm tra (Để tránh lỗi Cannot GET)
app.get("/", (req, res) => {
  res.send("Server SePay Webhook đang chạy trên Render!");
});

// 2. Route nhận Webhook từ SePay
app.post("/handlePaymentWebhook", async (req, res) => {
  try {
    const data = req.body;
    console.log("Dữ liệu từ SePay:", JSON.stringify(data));

    const content = data.content || "";
    const orderIdMatch = content.match(/ORDER_\d+/);
    const orderId = orderIdMatch ? orderIdMatch[0] : null;

    if (!orderId) {
      return res.status(200).send("Không thấy mã đơn hàng");
    }

    const orderRef = admin.firestore().collection("orders").doc(orderId);
    const doc = await orderRef.get();

    if (!doc.exists) {
      return res.status(200).send("Đơn hàng không tồn tại");
    }

    await orderRef.update({
      paymentStatus: "PAID",
      status: "Đã xác nhận",
      paidAt: admin.firestore.FieldValue.serverTimestamp()
    });

    console.log(`Đã cập nhật đơn hàng: ${orderId}`);
    return res.status(200).json({ success: true });

  } catch (error) {
    console.error("Lỗi:", error);
    return res.status(500).send("Internal Server Error");
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});