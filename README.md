# 🚗⚡ ParkEasy

> **The futuristic, neon-soaked solution to urban parking chaos.**

[![Android](https://img.shields.io/badge/Android-Java-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Firebase](https://img.shields.io/badge/Firebase-Firestore-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com/)
[![Status](https://img.shields.io/badge/Status-Active_Development-neon?style=for-the-badge&color=00F0FF)](https://github.com/)

## 💡 What is this?
**ParkEasy** isn't just a parking app; it's a **Smart Parking Ecosystem**. Built for the modern driver, it helps users find, book, and manage parking slots in real-time. 

Gone are the days of circling the block. With our **Live Sync** technology and **Cyberpunk Interface**, booking a spot is faster than ordering a pizza.

## ✨ Key Features

* **⚡ Real-Time Slot Sync:** Watch slots turn Red (Occupied) instantly across multiple devices. No refresh button needed—we aren't cavemen.
* **🎨 Cyberpunk UI:** A sleek, dark-themed aesthetic with neon gradients (`#00F0FF` & `#FF0055`) that makes parking feel like hacking the mainframe.
* **🧹 The "Janitor" System:** A smart background worker that automatically cleans up expired bookings when you open the dashboard. No "Ghost Cars" allowed.
* **🔐 Secure Auth:** Full Login/Registration system powered by Firebase Authentication.
* **📜 Digital Access Logs:** Track your active, completed, and cancelled bookings with a filterable history timeline.

## 🛠️ Tech Stack

* **Language:** Java (Native Android)
* **UI/UX:** XML with Custom Drawables, Gradients, and CardViews
* **Backend:** Google Firebase (Firestore Database)
* **Authentication:** Firebase Auth
* **Architecture:** MVC (Model-View-Controller)

## 📸 Screenshots
| **Dashboard** | **Slot Selection** | **Access Logs** |
|:---:|:---:|:---:|
| | | |
| *Futuristic Home* | *Live Grid View* | *History & Filters* |

*(Note: Don't forget to add actual screenshots here later!)*

## 🚀 How to Run Locally

1.  **Clone the Repo:**
    ```bash
    git clone [https://github.com/YourUsername/ParkEasy.git](https://github.com/YourUsername/ParkEasy.git)
    ```
2.  **Open in Android Studio:**
    File > Open > Select the `ParkEasy` folder.
3.  **🔥 IMPORTANT: Firebase Setup:**
    * This project relies on Firebase. You **MUST** add your own `google-services.json` file.
    * Go to [Firebase Console](https://console.firebase.google.com/).
    * Create a project -> Add Android App -> Download `google-services.json`.
    * Place the file in the `app/` folder.
4.  **Sync & Run:**
    * Hit `Sync Project with Gradle Files`.
    * Press the ▶️ Run button.

## 🧠 Under The Hood: The "Janitor" Logic

One of the coolest features is how we handle expired slots without a backend server. We use a **Client-Side Lazy Cleanup** method:

```java
// The "Janitor" runs silently when the dashboard loads
public static void freeExpiredSlots() {
    long now = System.currentTimeMillis();
    db.collection("slots")
      .whereEqualTo("occupied", true)
      .whereLessThan("expiryTime", now)
      .get()
      .addOnSuccessListener(snapshots -> {
          // 🧹 Sweeps away old data instantly
      });
}
🤝 Contributing
Got a wild idea? Found a bug?

Fork the Project.

Create your Feature Branch (git checkout -b feature/AmazingFeature).

Commit your Changes (git commit -m 'Add some AmazingFeature').

Push to the Branch (git push origin feature/AmazingFeature).

Open a Pull Request.

📄 License
Distributed under the MIT License. See LICENSE for more information.

<p align="center"> Built with ❤️, Java, and a lot of Caffeine by <b>AnonJJ</b> </p>
