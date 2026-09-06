"""Editorial story copy for five nature categories in ``categories.json``.

The mappings in this module are intentionally separate from the generated
catalogue so they can be reviewed and imported without rewriting source data.
Run this file directly to verify keys, whole-word coverage, and normal-case
English prose against the current asset.
"""

from __future__ import annotations

import json
import re
from pathlib import Path


CATEGORY_STORIES = {
    "animals": {
        "storyEn": (
            "At ten in the morning, children walk in a row along the old tar road "
            "to a farm for an animal art day. A cat balances on a log, a dog guards "
            "an oat sack, and a cow watches a hen.\n\nThen a crow lands near a rat while "
            "an ant and a bee explore the flowers. Rafi has been asked to act like a "
            "bear, so he cups one ear and growls.\n\nThe children are delighted when a "
            "frog appears through the fog. A tan goat got a shiny gold tag from the "
            "farmer, and its lively pose becomes the center of their mural."
        ),
        "storyBn": (
            "সকাল দশটায় শিশুরা প্রাণী নিয়ে ছবি আঁকার জন্য সারি বেঁধে পুরোনো "
            "পিচঢালা পথ ধরে একটি খামারে যায়। একটি বিড়াল গাছের গুঁড়ির ওপর "
            "ভারসাম্য রাখে, একটি কুকুর ওটসের বস্তা পাহারা দেয় এবং একটি গরু "
            "মুরগির দিকে তাকিয়ে থাকে।\n\nতারপর একটি কাক ইঁদুরের কাছে নামে, আর একটি "
            "পিঁপড়া ও মৌমাছি ফুলের চারপাশে ঘোরে। রাফিকে ভালুকের মতো অভিনয় করতে "
            "বলা হয়েছে, তাই সে এক কানে হাত দিয়ে গর্জন করে।\n\nকুয়াশার ভেতর থেকে "
            "একটি ব্যাঙ বেরিয়ে এলে শিশুরা আনন্দে মেতে ওঠে। হালকা বাদামি ছাগলটি "
            "খামারির কাছ থেকে সোনালি ট্যাগ পেয়েছে, আর তার প্রাণবন্ত ভঙ্গিই তাদের "
            "দেয়ালচিত্রের মূল বিষয় হয়।"
        ),
    },
    "fish": {
        "storyEn": (
            "During our hour at the riverside fair, Sami and his aunt visit a fish "
            "exhibit after arriving by car. She wears a tan cap and lets him feel the "
            "smooth model of an eel beside an old chart about cod from the cold sea.\n\n"
            "The exhibit has a carp, rohu, tuna, crab, hilsa, prawn, and shark, and the "
            "guide invites every child to ask a question. Outside, hail taps the sail "
            "of a small boat.\n\nAt the cooking stall, a vendor keeps each raw prawn "
            "chilled before cooking it in a clean pan. Nearby, an ant carries a piece "
            "of nut while the family waits for a cab home."
        ),
        "storyBn": (
            "নদীতীরের মেলায় আমাদের এক ঘণ্টা সময় কাটানোর সময় সামি ও তার খালা "
            "গাড়িতে এসে মাছের প্রদর্শনী দেখেন। খালার মাথায় হালকা বাদামি ক্যাপ; "
            "তিনি সামিকে ঠান্ডা সাগরের কড মাছের পুরোনো চার্টের পাশে রাখা ঈল মাছের "
            "মসৃণ নমুনাটি ছুঁয়ে অনুভব করতে দেন।\n\nপ্রদর্শনীতে কার্পজাতীয় মাছ, রোহু, "
            "টুনা, কাঁকড়া, ইলিশ, বড় চিংড়ি ও হাঙর আছে, আর পথপ্রদর্শক প্রতিটি "
            "শিশুকে প্রশ্ন করতে বলেন। বাইরে শিলাবৃষ্টির দানা একটি ছোট নৌকার পালে "
            "টুপটাপ পড়ে।\n\nরান্নার দোকানে বিক্রেতা প্রতিটি কাঁচা চিংড়ি ঠান্ডায় "
            "রেখে পরে পরিষ্কার কড়াইয়ে রান্না করেন। পরিবারটি বাড়ি ফেরার ট্যাক্সির "
            "জন্য অপেক্ষা করে, আর কাছেই একটি পিঁপড়া বাদামের টুকরা বয়ে নেয়।"
        ),
    },
    "birds": {
        "storyEn": (
            "After a gale, many children saw a man park his car by the wetland. A row "
            "of birds was waiting: a crow sat low near a cow, while a myna pecked at a "
            "yam and a swan crossed the water.\n\nA tiny wren inspected a new nest, and a "
            "crane stood on one leg beside a heron. An eagle circled above an eel in "
            "the shallows. Mira cupped her ear so she could hear an owl, while a robin "
            "hopped beside a model skeleton whose label pointed to a rib. The guide "
            "said birds are different at every age and can find food in clever ways.\n\n"
            "Back at the learning centre, the children study a piece of ore that "
            "contains iron and sort the display cards. Outside, a hen scratches the "
            "soil while a stork searches through leaves beginning to rot."
        ),
        "storyBn": (
            "ঝড়ো হাওয়ার পর অনেক শিশু দেখল, একজন পুরুষ জলাভূমির পাশে তার গাড়ি "
            "রাখছেন। সেখানে সারি বেঁধে পাখি ছিল: একটি কাক গরুর কাছে নিচু হয়ে "
            "বসেছিল, একটি ময়না মিষ্টি আলুতে ঠোকর দিচ্ছিল এবং একটি রাজহাঁস পানি "
            "পার হচ্ছিল।\n\nছোট একটি রেন পাখি নতুন বাসা দেখছিল, আর একটি সারস এক পায়ে "
            "বকের পাশে দাঁড়িয়েছিল। অগভীর পানির একটি বাইম মাছের ওপরে ঈগল চক্কর "
            "দিচ্ছিল। মিরা নিজের কানে হাত দিয়ে পেঁচার ডাক শোনে; এদিকে একটি রবিন "
            "পাখির কঙ্কালের মডেলের পাশে লাফাচ্ছিল, যার লেবেলে পাঁজরের একটি হাড় "
            "দেখানো ছিল। পথপ্রদর্শক বলেন, বয়সভেদে পাখিরা আলাদা হয় এবং তারা নানা "
            "বুদ্ধিমান উপায়ে খাবার খুঁজতে পারে।\n\nশিক্ষাকেন্দ্রে ফিরে শিশুরা "
            "লোহাযুক্ত আকরিকের একটি টুকরা দেখে এবং প্রদর্শনীর কার্ডগুলো ধরন অনুযায়ী "
            "সাজায়। বাইরে একটি মুরগি মাটি আঁচড়ায়, আর একটি মানিকজোড় পচতে থাকা "
            "পাতার মধ্যে খাবার খোঁজে।"
        ),
    },
    "flowers": {
        "storyEn": (
            "On a hot day, Mina and Ratul went out to help build a flower display at "
            "their school garden. They lifted the lid from a seed box and found a bud "
            "beside labels for a rose, aster, daisy, lotus, pansy, tulip, and orchid. "
            "They used a smooth piece of iron ore to keep the labels from blowing "
            "away. Ratul noticed that the aster on one label looked like a star.\n\n"
            "After Mina ate a pea from her plate and drank tea, she noticed a purple petal on her "
            "sleeve. She asked Ratul to say something about the lotus. He said it grows "
            "out of a muddy pond and needs a lot of sunlight.\n\nThe teacher told Mina "
            "to put a tulip bulb in a small pit with its tip facing up. A ladybird hid "
            "beneath a leaf, then crawled out and made Mina's lip curl into a smile. "
            "The rich donor who funded the garden gave them tools to rid the path of "
            "litter, and they did not leave any fallen flowers to rot. Everyone kept away "
            "from a sharp thorn."
        ),
        "storyBn": (
            "গরমের এক দিনে মিনা ও রাতুল বাইরে গিয়ে স্কুলের বাগানে ফুলের প্রদর্শনী "
            "তৈরি করতে সাহায্য করেছিল। তারা বীজের বাক্সের ঢাকনা তুলে একটি কুঁড়ি এবং "
            "গোলাপ, অ্যাস্টার, ডেইজি, পদ্ম, প্যানসি, টিউলিপ ও অর্কিডের লেবেল পেয়েছিল। "
            "লোহার আকরিকের একটি মসৃণ টুকরা দিয়ে তারা লেবেলগুলো বাতাসে উড়ে যাওয়া "
            "থেকে আটকেছিল। রাতুল লক্ষ করেছিল, একটি লেবেলের অ্যাস্টার ফুলটি দেখতে "
            "তারার মতো।\n\nমিনা "
            "থালা থেকে একটি মটর খেয়ে চা পান করার পর তার জামায় একটি বেগুনি পাপড়ি "
            "দেখতে পায়। সে রাতুলকে পদ্ম সম্পর্কে কিছু বলতে বলে। রাতুল বলেছিল, পদ্ম "
            "কাদামাখা পুকুরের ভেতর থেকে বাইরে বেরিয়ে আসে এবং এর অনেক সূর্যালোক "
            "দরকার।\n\nশিক্ষক মিনাকে একটি ছোট গর্তে টিউলিপের কন্দ ডগাটি ওপরের দিকে "
            "রেখে দিতে বলেছিলেন। একটি গুবরে পোকা পাতার নিচে লুকিয়েছিল; সেটি বেরিয়ে "
            "এলে মিনার ঠোঁটে হাসি ফুটেছিল। বাগানের খরচ দিয়েছিলেন এমন একজন ধনী দাতা "
            "পথকে আবর্জনামুক্ত করার জন্য তাদের সরঞ্জাম দিয়েছিলেন, আর তারা কোনো ঝরা "
            "ফুল পচতে দেয়নি। সবাই ধারালো কাঁটা থেকে দূরে ছিল।"
        ),
    },
    "fruits": {
        "storyEn": (
            "At a village fruit fair, a man in a cap gives his pal a basket that is a "
            "perfect fit for a gift. Inside are a fig, date, pear, apple, grape, lemon, "
            "one melon, mango, peach, and cherry.\n\nTwo men ate fruit with tea while a "
            "lone pea rested beside a heap of peels.\n\nA little girl held the basket in "
            "her lap, touched one ear, and began to cry when her cherry rolled away. "
            "The fruit sellers are kind, so each one helps her find it."
        ),
        "storyBn": (
            "গ্রামের ফলের মেলায় ক্যাপ পরা একজন পুরুষ তার বন্ধুকে এমন একটি ঝুড়ি "
            "দেন, যা উপহারের জন্য একেবারে ঠিক মাপের। ভেতরে একটি ডুমুর, খেজুর, "
            "নাশপাতি, আপেল, আঙুর, লেবু, একটি তরমুজ, আম, পিচ ও চেরি আছে।\n\nদুজন পুরুষ "
            "চায়ের সঙ্গে ফল খেয়েছেন, আর খোসার স্তূপের পাশে একটি মটর একা পড়ে আছে।\n\n"
            "একটি ছোট মেয়ে ঝুড়িটি কোলে ধরে, এক কানে হাত দেয় এবং তার চেরিটি গড়িয়ে "
            "গেলে কাঁদতে শুরু করে। ফল বিক্রেতারা দয়ালু, তাই প্রত্যেকে তাকে সেটি "
            "খুঁজে পেতে সাহায্য করেন।"
        ),
    },
}


LEVEL_STORIES = {
    "animals": {
        1: {
            "sentenceEn": (
                "Maya's cat curls up beside the stage. When the school play begins, "
                "it watches her act as a friendly farmer."
            ),
            "sentenceBn": (
                "মায়ার বিড়ালটি মঞ্চের পাশে গুটিসুটি হয়ে বসে। স্কুলের নাটক শুরু "
                "হলে সেটি মায়াকে বন্ধুসুলভ খামারির চরিত্রে অভিনয় করতে দেখে।"
            ),
        },
        2: {
            "sentenceEn": (
                "A cow grazes beside the fence. Above it, a crow joins a row of birds "
                "on the wire."
            ),
            "sentenceBn": (
                "একটি গরু বেড়ার পাশে ঘাস খায়। তার ওপরে তারের ওপর সারি বেঁধে বসা "
                "পাখিদের সঙ্গে একটি কাক যোগ দেয়।"
            ),
        },
        3: {
            "sentenceEn": (
                "Our old dog finds a log by the pond. Its wet fur glows gold in the "
                "afternoon sun."
            ),
            "sentenceBn": (
                "আমাদের বয়স্ক কুকুরটি পুকুরের পাশে একটি গাছের গুঁড়ি খুঁজে পায়। "
                "বিকেলের রোদে তার ভেজা লোম সোনালি রঙে ঝলমল করে।"
            ),
        },
        4: {
            "sentenceEn": (
                "The hen has laid ten eggs. Then the farmer places the eggs in a safe "
                "basket."
            ),
            "sentenceBn": (
                "মুরগিটি দশটি ডিম পেড়েছে। তারপর খামারি ডিমগুলো একটি নিরাপদ ঝুড়িতে "
                "রাখেন।"
            ),
        },
        5: {
            "sentenceEn": (
                "A rat scurries away from the fresh tar on the road. Later, Rumi draws "
                "the little animal in her art book."
            ),
            "sentenceBn": (
                "একটি ইঁদুর রাস্তার তাজা আলকাতরা থেকে দ্রুত সরে যায়। পরে রুমি তার "
                "আঁকার খাতায় ছোট প্রাণীটির ছবি আঁকে।"
            ),
        },
        6: {
            "sentenceEn": (
                "An ant crosses the sunny path. Its black body stands out against the "
                "tan dust."
            ),
            "sentenceBn": (
                "একটি পিঁপড়া রোদেলা পথ পার হয়। হালকা বাদামি ধুলোর ওপর তার কালো "
                "শরীরটি স্পষ্ট দেখা যায়।"
            ),
        },
        7: {
            "sentenceEn": (
                "A bee visits the mustard flowers. It has been busy collecting nectar "
                "all morning."
            ),
            "sentenceBn": (
                "একটি মৌমাছি সরিষার ফুলে বসে। সেটি সারা সকাল ফুলের মধুরস সংগ্রহে "
                "ব্যস্ত ছিল।"
            ),
        },
        8: {
            "sentenceEn": (
                "A bear turns one ear toward the river. We are quiet as it listens for "
                "splashing fish."
            ),
            "sentenceBn": (
                "একটি ভালুক এক কান নদীর দিকে ঘুরিয়ে দেয়। পানিতে মাছের ছলাৎ শব্দ "
                "শোনার সময় আমরা চুপচাপ থাকি।"
            ),
        },
        9: {
            "sentenceEn": (
                "In the fog, a frog waits beside the pond. Mina leaves a shallow water "
                "dish for the thirsty animal."
            ),
            "sentenceBn": (
                "কুয়াশার মধ্যে একটি ব্যাঙ পুকুরের পাশে অপেক্ষা করে। তৃষ্ণার্ত "
                "প্রাণীটির জন্য মিনা একটি অগভীর পানির পাত্র রেখে দেয়।"
            ),
        },
        10: {
            "sentenceEn": (
                "A goat got a blue tag from the farmer. It nibbles oat grain beside "
                "the barn."
            ),
            "sentenceBn": (
                "একটি ছাগল খামারির কাছ থেকে নীল ট্যাগ পেয়েছে। সেটি গোলাঘরের পাশে "
                "ওটসের দানা কুটকুট করে খায়।"
            ),
        },
    },
    "fish": {
        1: {
            "sentenceEn": (
                "A fish swims around Sami's boat. Its silver scales catch his eye."
            ),
            "sentenceBn": (
                "একটি মাছ সামির নৌকার চারপাশে সাঁতার কাটে। মাছটির রুপালি আঁশ তার "
                "চোখে পড়ে।"
            ),
        },
        2: {
            "sentenceEn": (
                "Nila gently touches a model eel. She can feel how its long body curves "
                "like a snake."
            ),
            "sentenceBn": (
                "নীলা ঈল মাছের একটি নমুনা আলতো করে ছোঁয়। ছুঁয়ে সে অনুভব করে, এর "
                "লম্বা শরীরটি কীভাবে সাপের মতো বাঁক নিয়েছে।"
            ),
        },
        3: {
            "sentenceEn": (
                "An old sailor shows us a picture of cod. The fish lives in the cold "
                "sea."
            ),
            "sentenceBn": (
                "একজন বয়স্ক নাবিক আমাদের কড মাছের ছবি দেখান। মাছটি ঠান্ডা সাগরে "
                "বাস করে।"
            ),
        },
        4: {
            "sentenceEn": (
                "A carp swims in a pond beside the road. Lila watches it from the car "
                "while wearing her red cap."
            ),
            "sentenceBn": (
                "রাস্তার পাশের পুকুরে একটি কার্পজাতীয় মাছ সাঁতার কাটে। লাল ক্যাপ "
                "পরা লীলা গাড়ির ভেতর থেকে সেটি দেখে।"
            ),
        },
        5: {
            "sentenceEn": (
                "Our class watches a rohu swim for an hour. Its silver body flashes in "
                "the pond."
            ),
            "sentenceBn": (
                "আমাদের শ্রেণির সবাই এক ঘণ্টা ধরে একটি রোহু মাছের সাঁতার দেখে। "
                "পুকুরে তার রুপালি শরীর ঝলমল করে।"
            ),
        },
        6: {
            "sentenceEn": (
                "My aunt wears a tan hat as she opens a tin of tuna for lunch. An ant "
                "finds a small piece of a nut beside her plate."
            ),
            "sentenceBn": (
                "আমার খালা দুপুরের খাবারের জন্য টুনা মাছের কৌটা খোলার সময় হালকা "
                "বাদামি টুপি পরেন। তাঁর থালার পাশে একটি পিঁপড়া বাদামের ছোট টুকরা "
                "খুঁজে পায়।"
            ),
        },
        7: {
            "sentenceEn": (
                "Our car needs a repair near the harbor. While we wait for a cab, a "
                "crab scuttles under the bridge."
            ),
            "sentenceBn": (
                "বন্দরের কাছে আমাদের গাড়ি মেরামত করা দরকার। আমরা ট্যাক্সির জন্য "
                "অপেক্ষা করার সময় একটি কাঁকড়া সেতুর নিচে পাশ ফিরে চলে।"
            ),
        },
        8: {
            "sentenceEn": (
                "A fisher has hilsa in his basket beneath the boat's sail. When hail "
                "begins to fall, he quickly covers the national fish."
            ),
            "sentenceBn": (
                "নৌকার পালের নিচে জেলের ঝুড়িতে তার ধরা ইলিশ মাছ আছে। শিলাবৃষ্টি "
                "শুরু হলে তিনি দ্রুত বাংলাদেশের জাতীয় মাছটি ঢেকে দেন।"
            ),
        },
        9: {
            "sentenceEn": (
                "A vendor keeps a raw prawn chilled at the market. At home, Auntie "
                "cooks it in a clean pan."
            ),
            "sentenceBn": (
                "একজন বিক্রেতা বাজারে কাঁচা বড় চিংড়িটি ঠান্ডায় রাখেন। বাড়িতে খালা "
                "সেটি পরিষ্কার কড়াইয়ে রান্না করেন।"
            ),
        },
        10: {
            "sentenceEn": (
                "The shark has rows of sharp teeth. We ask the guide how it finds food "
                "at sea."
            ),
            "sentenceBn": (
                "হাঙরটির সারি সারি ধারালো দাঁত আছে। আমরা পথপ্রদর্শককে জিজ্ঞাসা করি, "
                "সাগরে এটি কীভাবে খাবার খুঁজে পায়।"
            ),
        },
    },
    "birds": {
        1: {
            "sentenceEn": (
                "An owl flies low over the quiet field. It lands on a branch to watch "
                "for mice."
            ),
            "sentenceBn": (
                "একটি পেঁচা শান্ত মাঠের ওপর দিয়ে নিচু হয়ে ওড়ে। ইঁদুর খোঁজার জন্য "
                "এটি একটি ডালে বসে।"
            ),
        },
        2: {
            "sentenceEn": (
                "A crow stands in a row of birds above the cowshed. Below, a cow eats "
                "grass."
            ),
            "sentenceBn": (
                "গোয়ালঘরের ওপরে সারি বেঁধে বসা পাখিদের মধ্যে একটি কাক দাঁড়িয়ে "
                "আছে। নিচে একটি গরু ঘাস খায়।"
            ),
        },
        3: {
            "sentenceEn": (
                "A myna watches a man pull a yam from the soil. Many other birds gather "
                "nearby."
            ),
            "sentenceBn": (
                "একটি ময়না দেখে, একজন পুরুষ মাটি থেকে মিষ্টি আলু তুলছেন। আরও অনেক "
                "পাখি কাছে জড়ো হয়।"
            ),
        },
        4: {
            "sentenceEn": (
                "Rina saw a swan on the lake. It was gliding between two boats."
            ),
            "sentenceBn": (
                "রিনা হ্রদে একটি রাজহাঁস দেখেছিল। সেটি দুই নৌকার মাঝখান দিয়ে ভেসে "
                "যাচ্ছিল।"
            ),
        },
        5: {
            "sentenceEn": (
                "A wren carries grass to its new nest. The tiny bird sings from a "
                "bamboo fence."
            ),
            "sentenceBn": (
                "একটি রেন পাখি তার নতুন বাসায় ঘাস নিয়ে যায়। ছোট পাখিটি বাঁশের "
                "বেড়া থেকে গান গায়।"
            ),
        },
        6: {
            "sentenceEn": (
                "A crane stands beside a parked car with one leg raised. Its calls are "
                "loud, and Mina covers one ear. She can still watch its long beak."
            ),
            "sentenceBn": (
                "একটি সারস এক পা তুলে রাখা গাড়ির পাশে দাঁড়িয়ে আছে। পাখিটির ডাক "
                "জোরে হওয়ায় মিনা এক কান ঢেকে রাখে। তবু সে পাখিটির লম্বা ঠোঁট দেখতে "
                "পারে।"
            ),
        },
        7: {
            "sentenceEn": (
                "A strong gale blows above the river. An eagle grips a branch with one "
                "leg while an eel wriggles below. The number on the eagle's leg band "
                "helps researchers record its age."
            ),
            "sentenceBn": (
                "নদীর ওপর দিয়ে প্রবল ঝড়ো হাওয়া বয়ে যায়। একটি ঈগল এক পা দিয়ে ডাল "
                "আঁকড়ে ধরে, আর নিচে একটি বাইম মাছ এঁকেবেঁকে চলে। ঈগলের পায়ের "
                "বলয়ের নম্বরটি গবেষকদের তার বয়স নথিভুক্ত করতে সাহায্য করে।"
            ),
        },
        8: {
            "sentenceEn": (
                "Maya shows her brother a piece of ore beside the nature display. "
                "Outside, one heron fishes near the bank while a hen scratches the soil."
            ),
            "sentenceBn": (
                "প্রকৃতির প্রদর্শনীর পাশে মায়া তার ভাইকে এক টুকরো ধাতব আকরিক "
                "দেখায়। বাইরে একটি বক তীরের কাছে মাছ ধরে, আর একটি মুরগি মাটি "
                "আঁচড়ায়।"
            ),
        },
        9: {
            "sentenceEn": (
                "A robin hops beside a classroom model of a bird. The teacher points "
                "to one rib and explains that the wire inside is iron."
            ),
            "sentenceBn": (
                "একটি রবিন পাখি শ্রেণিকক্ষে রাখা পাখির মডেলের পাশে লাফায়। শিক্ষক "
                "একটি পাঁজরের হাড় দেখিয়ে বোঝান, ভেতরের তারটি লোহা দিয়ে তৈরি।"
            ),
        },
        10: {
            "sentenceEn": (
                "A stork searches for food among fallen leaves. We sort the wet leaves "
                "into a compost pile, where they can rot and feed the soil."
            ),
            "sentenceBn": (
                "একটি মানিকজোড় ঝরা পাতার মধ্যে খাবার খোঁজে। আমরা ভেজা পাতাগুলো "
                "বেছে কম্পোস্টের স্তূপে রাখি, যেখানে সেগুলো পচে মাটির পুষ্টি বাড়ায়।"
            ),
        },
    },
    "flowers": {
        1: {
            "sentenceEn": (
                "We build a simple protective box around the flowerpot. At night, we "
                "close the lid to keep the bud safe."
            ),
            "sentenceBn": (
                "আমরা ফুলের টব ঘিরে সুরক্ষার জন্য একটি সাধারণ বাক্স তৈরি করি। রাতে "
                "কুঁড়িটি নিরাপদ রাখতে ঢাকনা বন্ধ করি।"
            ),
        },
        2: {
            "sentenceEn": (
                "A rose grows beside a rock that contains iron ore. The flower's sweet "
                "scent makes the old mine garden feel cheerful."
            ),
            "sentenceBn": (
                "লোহার আকরিক থাকা একটি পাথরের পাশে গোলাপ ফুটেছে। ফুলটির মিষ্টি "
                "গন্ধ পুরোনো খনির বাগানকে আনন্দময় করে তোলে।"
            ),
        },
        3: {
            "sentenceEn": (
                "An aster opens beneath the evening star. Its purple petals shine in "
                "the fading light."
            ),
            "sentenceBn": (
                "সন্ধ্যার তারার নিচে একটি অ্যাস্টার ফুল ফোটে। ম্লান হয়ে আসা আলোয় "
                "তার বেগুনি পাপড়িগুলো উজ্জ্বল দেখায়।"
            ),
        },
        4: {
            "sentenceEn": (
                "One day, Lila found a daisy by the path. Her teacher asked her to say "
                "what its yellow center resembled. Lila said it looked like the sun."
            ),
            "sentenceBn": (
                "একদিন লীলা পথের পাশে একটি ডেইজি ফুল খুঁজে পেয়েছিল। শিক্ষক তাকে এর "
                "হলুদ মাঝখানটি কিসের মতো তা বলতে বলেছিলেন। লীলা বলেছিল, এটি সূর্যের "
                "মতো।"
            ),
        },
        5: {
            "sentenceEn": (
                "A lotus grows out of the muddy bed of a pond. A lot of sunlight helps "
                "its broad leaves grow."
            ),
            "sentenceBn": (
                "একটি পদ্ম পুকুরের কাদামাখা তলদেশ থেকে বেরিয়ে পানির ওপরে বাড়ে। "
                "অনেক সূর্যালোক তার চওড়া পাতাগুলোকে বাড়তে সাহায্য করে।"
            ),
        },
        6: {
            "sentenceEn": (
                "A pansy blooms after the rain. The children say they cannot find any "
                "dry leaves on it."
            ),
            "sentenceBn": (
                "বৃষ্টির পর একটি প্যানসি ফুল ফোটে। শিশুরা বলে, তারা ফুলটিতে কোনো "
                "শুকনো পাতা খুঁজে পাচ্ছে না।"
            ),
        },
        7: {
            "sentenceEn": (
                "Mina ate a pea from her plate while drinking tea. Then she noticed a "
                "purple petal beside the dish."
            ),
            "sentenceBn": (
                "চা পান করার সময় মিনা তার থালা থেকে একটি মটর খেয়েছিল। তারপর সে "
                "থালার পাশে একটি বেগুনি পাপড়ি দেখতে পায়।"
            ),
        },
        8: {
            "sentenceEn": (
                "On a hot day, the gardener shows us a thorn and warns us not to touch "
                "it. He explains that fallen flowers rot into compost."
            ),
            "sentenceBn": (
                "গরমের দিনে মালি আমাদের একটি কাঁটা দেখিয়ে সেটি ছুঁতে নিষেধ করেন। "
                "তিনি বোঝান, ঝরা ফুল পচে কম্পোস্ট হয়।"
            ),
        },
        9: {
            "sentenceEn": (
                "We put a tulip bulb into a small pit. Soon a green tip appears, and "
                "Rini's lip curls into a smile."
            ),
            "sentenceBn": (
                "আমরা একটি ছোট গর্তে টিউলিপের কন্দ রাখি। শিগগিরই একটি সবুজ ডগা "
                "দেখা দেয়, আর হাসিতে রিনির ঠোঁট বেঁকে ওঠে।"
            ),
        },
        10: {
            "sentenceEn": (
                "A rich donor brings an orchid to the community fair. Her child hid "
                "behind the flower table, then came out to help rid its leaves of dust."
            ),
            "sentenceBn": (
                "একজন ধনী দাতা কমিউনিটি মেলায় একটি অর্কিড আনেন। তাঁর শিশু ফুলের "
                "টেবিলের পেছনে লুকিয়েছিল, তারপর বেরিয়ে এসে পাতাগুলোকে ধুলামুক্ত "
                "করতে সাহায্য করে।"
            ),
        },
    },
    "fruits": {
        1: {
            "sentenceEn": (
                "A ripe fig will fit inside the small gift box. Rafi gives the sweet "
                "fruit to his grandmother."
            ),
            "sentenceBn": (
                "একটি পাকা ডুমুর ছোট উপহারের বাক্সে ঠিকমতো ধরে যাবে। রাফি মিষ্টি "
                "ফলটি তার দাদিকে দেয়।"
            ),
        },
        2: {
            "sentenceEn": (
                "Mina ate a date with her afternoon tea. The sweet brown fruit came "
                "from a palm tree."
            ),
            "sentenceBn": (
                "মিনা বিকেলের চায়ের সঙ্গে একটি খেজুর খেয়েছিল। মিষ্টি বাদামি ফলটি "
                "খেজুরগাছে জন্মেছিল।"
            ),
        },
        3: {
            "sentenceEn": (
                "A bee buzzes near Rumi's ear at lunchtime. A pear and a pea are in her "
                "lunchbox."
            ),
            "sentenceBn": (
                "দুপুরের খাবারের সময় রুমির কানের কাছে একটি মৌমাছি গুনগুন করে। তার "
                "খাবারের বাক্সে একটি নাশপাতি ও একটি মটর আছে।"
            ),
        },
        4: {
            "sentenceEn": (
                "A green pea rolls from Sami's lap while he shares an apple with his "
                "pal. They laugh and pick it up."
            ),
            "sentenceBn": (
                "বন্ধুর সঙ্গে আপেল ভাগ করার সময় সামির কোল থেকে একটি সবুজ মটর "
                "গড়িয়ে পড়ে। তারা হেসে সেটি তুলে নেয়।"
            ),
        },
        5: {
            "sentenceEn": (
                "Nila tucks her hair behind one ear before lunch. A pear, a pea, and one "
                "grape are on her plate."
            ),
            "sentenceBn": (
                "দুপুরের খাবারের আগে নীলা এক কানের পেছনে চুল গুঁজে দেয়। তার থালায় "
                "একটি নাশপাতি, একটি মটর ও একটি আঙুর আছে।"
            ),
        },
        6: {
            "sentenceEn": (
                "One lone melon sits beside a basket of lemons at the market. Two men "
                "carry it home and make a sweet melon drink with one lemon slice."
            ),
            "sentenceBn": (
                "বাজারে লেবুর ঝুড়ির পাশে একটি তরমুজ একা পড়ে আছে। দুজন পুরুষ সেটি "
                "বাড়িতে নিয়ে একটি লেবুর টুকরা দিয়ে মিষ্টি তরমুজের শরবত বানান।"
            ),
        },
        7: {
            "sentenceEn": (
                "A man picks a ripe mango from the tree. He smiles at its sweet summer "
                "smell."
            ),
            "sentenceBn": (
                "একজন পুরুষ গাছ থেকে একটি পাকা আম পাড়েন। গ্রীষ্মের ফলটির মিষ্টি "
                "গন্ধে তিনি হাসেন।"
            ),
        },
        8: {
            "sentenceEn": (
                "Two men find one lemon beside a large melon. The lone lemon adds a "
                "sour taste to their fruit salad."
            ),
            "sentenceBn": (
                "দুজন পুরুষ একটি বড় তরমুজের পাশে একটি লেবু পান। একা থাকা লেবুটি "
                "তাঁদের ফলের সালাদে টক স্বাদ যোগ করে।"
            ),
        },
        9: {
            "sentenceEn": (
                "A child in a blue cap carries a peach and a pea to the picnic. She "
                "gives each friend a peach slice, then places the single pit on the "
                "compost heap."
            ),
            "sentenceBn": (
                "নীল ক্যাপ পরা একটি শিশু বনভোজনে একটি পিচ ফল ও একটি মটর নিয়ে যায়। "
                "সে প্রত্যেক বন্ধুকে পিচের এক টুকরা দেয়, তারপর একটিমাত্র আঁটি "
                "কম্পোস্টের স্তূপে রাখে।"
            ),
        },
        10: {
            "sentenceEn": (
                "Mina drops a cherry from her bowl. Her brother sees her cry and brings "
                "the tiny red fruit back."
            ),
            "sentenceBn": (
                "মিনার বাটি থেকে একটি চেরি পড়ে যায়। তার ভাই তাকে কাঁদতে দেখে ছোট "
                "লাল ফলটি ফিরিয়ে আনে।"
            ),
        },
    },
}


_TARGET_CATEGORIES = {"animals", "fish", "birds", "flowers", "fruits"}
_UPPERCASE_WORD = re.compile(r"\b[A-Z]{2,}\b")
_SENTENCE_END = re.compile(r"[.!?](?=\s|$)")


def _contains_whole_word(text: str, word: str) -> bool:
    """Return whether an ASCII catalog word occurs as a whole word."""

    pattern = rf"(?<![A-Za-z]){re.escape(word)}(?![A-Za-z])"
    return re.search(pattern, text, flags=re.IGNORECASE) is not None


def validate_content(asset_path: Path | str | None = None) -> None:
    """Assert that the mappings match and cover the selected live catalog."""

    path = Path(asset_path) if asset_path else (
        Path(__file__).resolve().parents[1]
        / "app"
        / "src"
        / "main"
        / "assets"
        / "categories.json"
    )
    catalog = json.loads(path.read_text(encoding="utf-8"))
    categories = {
        category["id"]: category
        for category in catalog["categories"]
        if category["id"] in _TARGET_CATEGORIES
    }

    assert set(categories) == _TARGET_CATEGORIES, "selected asset categories changed"
    assert set(CATEGORY_STORIES) == _TARGET_CATEGORIES, "category story keys differ"
    assert set(LEVEL_STORIES) == _TARGET_CATEGORIES, "level story categories differ"

    for category_id, category in categories.items():
        category_copy = CATEGORY_STORIES[category_id]
        assert set(category_copy) == {"storyEn", "storyBn"}
        story_en = category_copy["storyEn"]
        story_bn = category_copy["storyBn"]
        assert story_bn.strip(), f"{category_id}: blank Bengali story"
        paragraphs_en = story_en.split("\n\n")
        paragraphs_bn = story_bn.split("\n\n")
        assert 2 <= len(paragraphs_en) <= 3, (
            f"{category_id}: expected 2-3 English paragraphs"
        )
        assert len(paragraphs_bn) == len(paragraphs_en), (
            f"{category_id}: Bengali paragraph count differs from English"
        )
        assert all(paragraph.strip() for paragraph in paragraphs_en + paragraphs_bn), (
            f"{category_id}: blank story paragraph"
        )
        assert not _UPPERCASE_WORD.search(story_en), (
            f"{category_id}: uppercase stuffing in category story"
        )
        missing_story_words = [
            word
            for word in category["storyWords"]
            if not _contains_whole_word(story_en, word)
        ]
        assert not missing_story_words, (
            f"{category_id}: category story misses {missing_story_words}"
        )

        asset_levels = {level["id"]: level for level in category["levels"]}
        assert set(LEVEL_STORIES[category_id]) == set(asset_levels), (
            f"{category_id}: level keys differ from asset"
        )
        for level_id, level in asset_levels.items():
            level_copy = LEVEL_STORIES[category_id][level_id]
            assert set(level_copy) == {"sentenceEn", "sentenceBn"}
            sentence_en = level_copy["sentenceEn"]
            assert level_copy["sentenceBn"].strip(), (
                f"{category_id}/{level_id}: blank Bengali story"
            )
            assert not _UPPERCASE_WORD.search(sentence_en), (
                f"{category_id}/{level_id}: uppercase stuffing"
            )
            sentence_count = len(_SENTENCE_END.findall(sentence_en))
            assert 2 <= sentence_count <= 4, (
                f"{category_id}/{level_id}: expected 2-4 sentences, got "
                f"{sentence_count}"
            )
            missing_level_words = [
                word
                for word in level["words"]
                if not _contains_whole_word(sentence_en, word)
            ]
            assert not missing_level_words, (
                f"{category_id}/{level_id}: mini-story misses {missing_level_words}"
            )


if __name__ == "__main__":
    validate_content()
    print("Editorial nature copy validation passed.")
