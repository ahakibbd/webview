# অফলাইন স্ক্যানার (Android Studio প্রজেক্ট)

ছবি থেকে লেখা পড়ে টেক্সট বানায়: **বাংলা, English, العربية**। ইঞ্জিন Tesseract।
ভাষার ডাটা APK-র ভেতরেই থাকে, তাই ফোনে ইন্টারনেট বা ডাটা লাগে না। অ্যাপে ইন্টারনেট পারমিশনও নেই।

## APK বানানোর ধাপ

1. Android Studio খুলে **File → Open** দিয়ে এই `OfflineScanner` ফোল্ডারটি খুলুন।
2. নিচে “Sync” চলতে দিন। **প্রথমবার পিসিতে ইন্টারনেট লাগবে**, কারণ:
   - Gradle লাইব্রেরি নামাবে,
   - বিল্ডের সময় তিনটি ভাষার ফাইল (`ben`, `eng`, `ara`) GitHub থেকে নেমে `app/src/main/assets/tessdata/` ফোল্ডারে বসবে।
3. মেনু থেকে **Build → Build Bundle(s) / APK(s) → Build APK(s)**।
4. শেষ হলে নিচে “locate” চাপুন। ফাইলটি থাকে:
   `app/build/outputs/apk/debug/app-debug.apk`
5. APK ফোনে পাঠিয়ে ইনস্টল করুন (ফোনে “অজানা উৎস থেকে ইনস্টল” অনুমতি দিতে হতে পারে)।
   অথবা ফোন USB দিয়ে লাগিয়ে (USB debugging চালু করে) সরাসরি **Run ▶** চাপুন।

## সম্ভাব্য সমস্যা

- **Android Studio “Update AGP / Gradle” দেখালে** “Upgrade” দিন। আপনার Studio নতুন হলে এটা স্বাভাবিক।
- **ভাষার ফাইল নামেনি** (বিল্ড ত্রুটিতে বার্তা আসবে): নিচের তিনটি লিংক থেকে হাতে নামিয়ে `app/src/main/assets/tessdata/` ফোল্ডার বানিয়ে তাতে রাখুন, তারপর আবার বিল্ড করুন।
  - https://github.com/tesseract-ocr/tessdata_fast/raw/main/ben.traineddata
  - https://github.com/tesseract-ocr/tessdata_fast/raw/main/eng.traineddata
  - https://github.com/tesseract-ocr/tessdata_fast/raw/main/ara.traineddata
- অ্যাপে “ভাষার ফাইল APK-তে নেই” দেখালে ওপরের কাজটি করে আবার বিল্ড করুন।

## কাস্টমাইজ

- **আরও ভাষা (হিন্দি, উর্দু ইত্যাদি):** `app/build.gradle.kts`-এ `tessLanguages` তালিকায় কোড যোগ করুন (যেমন `"hin"`)। তারপর `MainActivity.kt` ও `activity_main.xml`-এ একটি করে চেকবক্স যোগ করতে হবে।
- **বেশি নির্ভুলতা:** `tessRepo = "tessdata_best"` করুন। APK বড় হবে ও স্ক্যান ধীর হবে।
- **অ্যাপের নাম/আইডি:** নাম `res/values/strings.xml`-এ, আইডি `app/build.gradle.kts`-এর `applicationId`-তে।

## জেনে রাখুন

- ছাপা লেখায় ভালো চলে, হাতের লেখায় ভুল বেশি হয়।
- ছবি সোজা, পরিষ্কার ও যথেষ্ট আলোয় তুললে ফল ভালো হয়।
- বাংলা ও আরবির যুক্তাক্ষর/হরকতে কিছু ভুল আসতে পারে। পাওয়া লেখা টেক্সট বক্সে ঠিক করে নেওয়া যায়।
- এটি Google Lens নয়। অনুবাদ বা ছবি চেনা এতে নেই, শুধু লেখা পড়া।
