package com.example.tts

data class NewsTemplate(
    val id: String,
    val title: String,
    val filePrefix: String,
    val sampleScript: String
)

object NewsTemplates {
    val templates: List<NewsTemplate> = listOf(
        NewsTemplate(
            id = "reporter_hasan_news",
            title = "Reporter Hasan News (ব্রেকিং নিউজ)",
            filePrefix = "News_Script",
            sampleScript = "শুভ সন্ধ্যা, রিপোর্টার হাসান নিউজ ডেস্কে আপনাদের স্বাগতম। আজ দেশের তথ্যপ্রযুক্তি খাতে এক নতুন দিগন্তের সূচনা হয়েছে। ইন্টারনেট ছাড়াই সম্পূর্ণ অফলাইনে স্পষ্ট বাংলা সংবাদ পাঠের প্রযুক্তি এখন আপনার হাতের মুঠোয়! এই প্রযুক্তিতে ১২,৫০০ টাকার সাশ্রয়ী ডিভাইসেও মাত্র ১২.৫ সেকেন্ডে দীর্ঘ প্রতিবেদন তৈরি করা সম্ভব।"
        ),
        NewsTemplate(
            id = "breaking_news_voice",
            title = "Breaking News Alert (জরুরি সংবাদ)",
            filePrefix = "Breaking_News_Voice",
            sampleScript = "এইমাত্র পাওয়া খবর! রাজধানীসহ সারাদেশে আগামীকাল সকাল থেকে আবহাওয়ার বিশেষ পরিবর্তনের পূর্বাভাস দিয়েছে আবহাওয়া অধিদপ্তর। নদীবেষ্টিত অঞ্চলগুলোতে সতর্কতা জারি করা হয়েছে, এবং সবাইকে নিরাপদ আশ্রয়ে থাকার অনুরোধ জানানো হচ্ছে।"
        ),
        NewsTemplate(
            id = "special_investigative",
            title = "Special Report (বিশেষ প্রতিবেদন)",
            filePrefix = "Special_Report",
            sampleScript = "আজকের বিশেষ প্রতিবেদনে থাকছে গ্রামীণ অর্থনীতির ঘুরে দাঁড়ানোর গল্প। কঠোর পরিশ্রম আর আধুনিক কৃষি প্রযুক্তির ছোঁয়ায় বদলে গেছে হাজারো কৃষকের জীবন। র'ক্তের ঘাম পায়ে ফেলে যারা ফসল ফলান, তাঁদের মুখেই আজ সাফল্যের হাসি।"
        ),
        NewsTemplate(
            id = "sports_bulletin",
            title = "Sports Desk (খেলার সংবাদ)",
            filePrefix = "Sports_Bulletin",
            sampleScript = "খেলার সংবাদে স্বাগতম। শ্বাসরুদ্ধকর ফাইনাল ম্যাচে শেষ ওভারের দুর্দান্ত পারফরম্যান্সে জয় ছিনিয়ে নিয়েছে বাংলাদেশ দল! গ্যালারিভর্তি দর্শকের উচ্ছ্বাসে মুখরিত হয়ে উঠেছে পুরো স্টেডিয়াম।"
        ),
        NewsTemplate(
            id = "custom_script",
            title = "Custom Script (নিজের স্ক্রিপ্ট লিখুন)",
            filePrefix = "Custom_Voice",
            sampleScript = ""
        )
    )
}
