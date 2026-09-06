"""Hand-edited bilingual story copy for everyday-life categories.

Run this module directly to verify that its keys match the current category
catalog and that every required English word appears as a whole word in normal
case.
"""

from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any


CATEGORY_STORIES = {
    "vegetables": {
        "storyEn": (
            "On a breezy morning, a girl named Mili joins us as we walk with our dog and cat to a village garden. "
            "The car waits beside a wooden cart, while a rat peeks from a heap of pea pods. "
            "Fresh air moves through the field, where a bamboo rod supports a gourd vine and an "
            "oak sapling grows beside rows of okra. By the river, a boatman rests his oar beside a basket "
            "of yam.\n\n"
            "Mili may pick one carrot for lunch. She finds garlic, ginger, potato, radish, and "
            "turnip too, then places an egg on top of the basket. Her brother had brought a pot, "
            "and he has a clean rag for wiping it.\n\n"
            "A pony pulls the cart home. Mili holds its rein and gives the pony a gentle pat; the "
            "ring on her finger catches the sun, and she smiles with a grin. Her brother tucks his "
            "hair under a cap and warns her not to run near the road. At the final turn of the trip, "
            "they put every vegetable safely in the cart."
        ),
        "storyBn": (
            "হাওয়াভরা এক সকালে মিলি নামের একটি মেয়ে আমাদের সঙ্গে গ্রামের সবজিবাগানে যায়; আমরা আমাদের কুকুর ও বিড়ালকেও সঙ্গে নিই। "
            "গাড়িটি একটি কাঠের ঠেলাগাড়ির পাশে দাঁড়িয়ে থাকে, আর একটি ইঁদুর মটরশুঁটির খোসার স্তূপ থেকে উঁকি দেয়। "
            "খেতের ওপর দিয়ে নির্মল বাতাস বয়ে যায়; সেখানে বাঁশের একটি দণ্ড লাউয়ের লতা ধরে রেখেছে এবং একটি ওক গাছের "
            "চারা ঢেঁড়সের সারির পাশে বেড়ে উঠছে। নদীর ধারে এক মাঝি মিষ্টি আলুর ঝুড়ির পাশে তার দাঁড়টি রেখে বিশ্রাম নেয়।\n\n"
            "মিলির মা বলেন, সে দুপুরের খাবারের জন্য একটি গাজর তুলতে পারে। মিলি রসুন, আদা, আলু, মূলা ও শালগমও খুঁজে "
            "পায়, তারপর ঝুড়ির ওপরে একটি ডিম রাখে। তার ভাই একটি হাঁড়ি এনেছিল, আর সেটি মুছতে তার কাছে পরিষ্কার ন্যাকড়া আছে।\n\n"
            "একটি টাট্টুঘোড়া ঠেলাগাড়িটি বাড়ির দিকে টানে। মিলি লাগাম ধরে ঘোড়াটির গায়ে আলতো চাপড় দেয়; তার আঙুলের "
            "আংটি রোদে ঝিলমিল করে এবং সে দাঁত বের করে হাসে। তার ভাই নিজের চুল টুপির নিচে গুঁজে তাকে রাস্তার কাছে না "
            "দৌড়াতে বলে। ভ্রমণের শেষ মোড়ে তারা সব সবজি নিরাপদে ঠেলাগাড়িতে রাখে।"
        ),
    },
    "trees_plants": {
        "storyEn": (
            "Three children ride a bus to the botanical garden, and Nila brings her map. She and "
            "her pal sit beneath a palm tree, with the map open on her lap. Nearby, two men drink "
            "tea in the shade of a neem and study the strong trunk of a teak tree.\n\n"
            "A guide shows them an herb garden. One root has stayed too wet and begun to rot, but "
            "a fern, a fig plant, and a thick bush look healthy. Nila touches a fern with one finger, "
            "then turns the ring on that finger while a fish lifts its fin in the pond.\n\n"
            "They ate before the trip, yet the guide invites them to eat a nut and take a cup of tea. "
            "Under a garden shelter, a small gas stove warms the kettle, a rag keeps its handle clean, "
            "and green grass covers the ground beyond. For a garden game, the children run to the old "
            "tree, turn around its trunk, and carry a nut to a painted urn. After the game, they board the bus."
        ),
        "storyBn": (
            "তিনটি শিশু বাসে করে উদ্ভিদ উদ্যানে যায়, আর নীলা তার মানচিত্র সঙ্গে নেয়। সে ও তার বন্ধু একটি তালগাছের "
            "নিচে বসে, আর মানচিত্রটি তার কোলে খোলা থাকে। কাছেই দুজন পুরুষ একটি নিম ও একটি সেগুনগাছের ছায়ায় চা পান "
            "করেন এবং সেগুনের শক্ত গুঁড়িটি দেখেন।\n\n"
            "একজন পথপ্রদর্শক তাদের ভেষজ বাগান দেখান। একটি শিকড় অতিরিক্ত ভেজা থাকায় পচতে শুরু করেছে, কিন্তু ফার্ন, "
            "ডুমুরগাছ ও ঘন ঝোপটি সতেজ আছে। নীলা এক আঙুল দিয়ে ফার্ন ছোঁয়, তারপর সেই আঙুলের আংটিটি ঘোরায়; এদিকে "
            "পুকুরে একটি মাছ তার পাখনা তুলে সাঁতরে যায়।\n\n"
            "তারা ভ্রমণের আগে খেয়েছিল, তবু পথপ্রদর্শক তাদের একটি বাদাম খেতে ও এক কাপ চা নিতে বলেন। বাগানের ছাউনির "
            "নিচে ছোট গ্যাসের চুলায় কেটলি গরম হয়, একটি ন্যাকড়া দিয়ে তার হাতল পরিষ্কার রাখা হয়, আর ছাউনির বাইরে মাটি "
            "সবুজ ঘাসে ঢাকা। বাগানের খেলায় শিশুরা দৌড়ে পুরোনো গাছটির কাছে যায়, গুঁড়িটির চারদিকে ঘুরে একটি বাদাম "
            "রাঙানো কলসিতে নিয়ে যায়। খেলা শেষে তারা বাসে ওঠে।"
        ),
    },
    "body_parts": {
        "storyEn": (
            "Every child at the village health camp learns about the body. Mina raises one arm while "
            "a farmer gently guides a ram past the tent. The nurse says our eyes and ears are very "
            "useful: an eye helps us see, and an ear helps us hear. We are careful to protect them.\n\n"
            "Outside, a gale shakes the trees. A grandfather says that at his age one leg and his "
            "back feel stiff after a long ride in a cab. He takes a sip of water, wipes "
            "a drop from his lip, and watches his step so he does not slip.\n\n"
            "Near the farm stall, a nice hen stands beside another chicken. Mina lifts her chin, turns "
            "her neck, and feels fresh air move through her hair. She holds her father's hand and "
            "remembers that he had brought a scarf. "
            "At the end, a father and son play a smelling game. The son closes one eye and uses his "
            "nose to identify a lemon."
        ),
        "storyBn": (
            "গ্রামের স্বাস্থ্যশিবিরে প্রতিটি শিশু শরীর সম্পর্কে শেখে। মিনা একটি বাহু তোলে, আর এক কৃষক তাঁবুর পাশ দিয়ে "
            "একটি পুরুষ ভেড়াকে ধীরে নিয়ে যান। নার্স বলেন, আমাদের চোখ ও কান খুব কাজে লাগে: চোখ দিয়ে আমরা দেখি এবং কান "
            "দিয়ে শুনি। আমরা এগুলো যত্নে রাখি।\n\n"
            "বাইরে ঝোড়ো হাওয়ায় গাছগুলো দুলছে। এক দাদা বলেন, তাঁর এই বয়সে দীর্ঘ সময় ট্যাক্সিতে বসে আসার পর তাঁর একটি "
            "পা ও পিঠ শক্ত লাগে। তিনি পানিতে ছোট্ট চুমুক দেন, ঠোঁটের ফোঁটা মুছে নেন এবং যেন "
            "পা পিছলে না যায় সেদিকে খেয়াল রাখেন।\n\n"
            "খামারের দোকানের কাছে একটি শান্ত মাদি মুরগি আরেকটি মুরগির পাশে দাঁড়িয়ে আছে। মিনা চিবুক উঁচু করে ঘাড় "
            "ঘোরায় এবং চুলের ভেতর দিয়ে নির্মল বাতাস বয়ে যেতে অনুভব করে। সে বাবার হাত ধরে মনে করে, বাবা একটি স্কার্ফ "
            "এনেছিলেন। "
            "শেষে এক বাবা ও তাঁর ছেলে গন্ধ চেনার খেলা খেলে। ছেলেটি একটি চোখ বন্ধ করে নাক দিয়ে লেবুর গন্ধ চিনে নেয়।"
        ),
    },
    "colors": {
        "storyEn": (
            "At the school art fair, Nila paints a red flower beside a deer and an old dog with a gold "
            "collar. She fastens a pink ribbon with a pin, then uses black ink to draw a cab arriving "
            "at a science lab. On the back of her paper, she paints a brown boat.\n\n"
            "Now each child displays their own picture in a row. Rafi won a prize for a white duck "
            "standing in wet grass. A gust hit the display, but he caught the paper with both hands "
            "before it fell.\n\n"
            "A big, bright sign points right, and a bit of silver ribbon hangs from its edge. One child "
            "in an orange cap tells how she ran over earlier to fix it; she tucked loose hair behind one "
            "ear. The teachers are nearby to help. Beside the stage, a pup in a purple scarf drinks pure water and stays "
            "with its owner, following the fair's rule. In the final mural, silver fish live below a "
            "moon that begins to rise while a dancer lifts a thin veil from her face."
        ),
        "storyBn": (
            "স্কুলের শিল্পমেলায় নীলা একটি হরিণের পাশে লাল ফুল এবং সোনালি কলার পরা একটি বয়স্ক কুকুর আঁকে। সে পিন "
            "দিয়ে গোলাপি ফিতা আটকায়, তারপর কালো কালি দিয়ে বিজ্ঞান গবেষণাগারে পৌঁছানো একটি ট্যাক্সি আঁকে। কাগজের "
            "পেছনে সে একটি বাদামি নৌকা আঁকে।\n\n"
            "এখন প্রতিটি শিশু নিজের ছবি একটি সারিতে সাজায়। ভেজা ঘাসে দাঁড়ানো সাদা হাঁস এঁকে রাফি পুরস্কার জিতেছে। "
            "হঠাৎ বাতাসের ঝাপটা প্রদর্শনীতে আঘাত করে, কিন্তু কাগজটি পড়ার আগেই সে দুই হাতে ধরে ফেলে।\n\n"
            "একটি বড় উজ্জ্বল চিহ্ন ডান দিকে নির্দেশ করে, আর তার কিনারা থেকে রুপালি ফিতার ছোট্ট টুকরো ঝুলে থাকে। কমলা "
            "টুপি পরা একটি শিশু বলে, সে একটু আগে দৌড়ে এসে সেটি ঠিক করেছে; তখন সে আলগা চুল এক কানের পেছনে গুঁজে নিয়েছিল। "
            "শিক্ষকেরা সাহায্যের জন্য কাছেই আছেন। মঞ্চের পাশে বেগুনি স্কার্ফ পরা একটি কুকুরছানা বিশুদ্ধ পানি পান করে এবং মেলার নিয়ম মেনে মালিকের সঙ্গে "
            "থাকে। শেষ দেয়ালচিত্রে রুপালি মাছ সাঁতরে বেঁচে থাকে, আর চাঁদ ওপরে ওঠার সময় এক নৃত্যশিল্পী মুখ থেকে পাতলা "
            "ঘোমটা সরায়।"
        ),
    },
    "family": {
        "storyEn": (
            "At our family picnic by a nature park, Aunt Shila shows a video from her son, who is "
            "playing in snow abroad. Now everyone can wave to him. An ant crawls toward a tuna "
            "sandwich, and a girl moves the food away. A child who hid behind the car returns with "
            "a rag to wipe up some garlic sauce, then closes the jar's lid.\n\n"
            "Our elder cousin led the twins along a red path past a deer enclosure and an eel pond. "
            "He asks them to sit if they need a rest. One twin opens a tin of biscuits; its lid shines "
            "in the sun, and both children share a snack before they hope to win the family quiz.\n\n"
            "To finish the family game, each pair must find a set of picture cards. My sister and her "
            "friend search under a fern, then ride their bicycles back to the picnic mat. My brother "
            "brings hot tea and an herb salad made with yam. He asks if he may fly a kite after he "
            "helps lay the plates on the mat."
        ),
        "storyBn": (
            "প্রকৃতি উদ্যানের পাশে আমাদের পারিবারিক বনভোজনে শীলা খালা বিদেশে তুষারের মধ্যে খেলতে থাকা তাঁর ছেলের "
            "ভিডিও দেখান। এখন সবাই তাকে হাত নেড়ে শুভেচ্ছা জানাতে পারে। একটি পিঁপড়া টুনা মাছের স্যান্ডউইচের দিকে "
            "এগোয়, আর একটি মেয়ে খাবারটি সরিয়ে নেয়। গাড়ির পেছনে লুকিয়ে থাকা শিশুটি রসুনের সস মুছতে ন্যাকড়া নিয়ে "
            "ফিরে আসে, তারপর বয়ামের ঢাকনা বন্ধ করে।\n\n"
            "আমাদের বড় চাচাতো ভাই যমজ শিশু দুটিকে একটি লাল পথ ধরে হরিণের ঘের ও বাইম মাছের পুকুরের পাশ দিয়ে নিয়ে যায়। "
            "বিশ্রাম দরকার হলে সে তাদের বসতে বলে। এক যমজ শিশু বিস্কুটের টিন খোলে; টিনটির ঢাকনা রোদে ঝলমল করে, আর "
            "দুজনেই নাশতা ভাগ করে খেয়ে পারিবারিক প্রশ্নোত্তর খেলায় জিততে চায়।\n\n"
            "পারিবারিক খেলা শেষ করতে প্রতিটি জুটিকে ছবির তাসের একটি সেট খুঁজে পেতে হবে। আমার বোন ও তার বন্ধু একটি "
            "ফার্নগাছের নিচে খোঁজে, তারপর সাইকেলে চড়ে বনভোজনের মাদুরে ফিরে আসে। আমার ভাই মিষ্টি আলু দিয়ে বানানো ভেষজ "
            "সালাদ ও গরম চা আনে। সে জানতে চায়, মাদুরে থালাগুলো সাজিয়ে রাখার পর সে ঘুড়ি ওড়াতে পারে কি না।"
        ),
    },
}


LEVEL_STORIES = {
    "vegetables": {
        1: {
            "sentenceEn": "Rina opens one pod and takes out a green pea for lunch. She places the single empty pod beside a heap of garden leaves.",
            "sentenceBn": "রিনা একটি খোসা খুলে দুপুরের খাবারের জন্য একটি সবুজ মটরশুঁটি বের করে। সে ওই একটিমাত্র খালি খোসা বাগানের পাতার স্তূপের পাশে রাখে।",
        },
        2: {
            "sentenceEn": "Hasan finds a yam at the market. His mother says he may choose it for their curry.",
            "sentenceBn": "হাসান বাজারে একটি মিষ্টি আলু খুঁজে পায়। তার মা বলেন, সে তরকারির জন্য এটি বেছে নিতে পারে।",
        },
        3: {
            "sentenceEn": "Okra grows beside a young oak in the school garden. An oar rests in a boat by the nearby pond.",
            "sentenceBn": "স্কুলের বাগানে একটি ছোট ওক গাছের পাশে ঢেঁড়স জন্মায়। কাছের পুকুরে একটি নৌকার ভেতর দাঁড় রাখা আছে।",
        },
        4: {
            "sentenceEn": "Our dog naps well away from a gourd vine. A bamboo rod supports the vine firmly inside the garden fence.",
            "sentenceBn": "আমাদের কুকুরটি লাউয়ের লতা থেকে নিরাপদ দূরত্বে ঘুমায়। বাগানের বেড়ার ভেতরে বাঁশের একটি দণ্ড লতাটিকে শক্তভাবে ধরে রাখে।",
        },
        5: {
            "sentenceEn": "A bull pulls a cart carrying carrot baskets from the market while our car waits nearby. A cat watches a rat dart behind the cart.",
            "sentenceBn": "একটি বলদ গাজরের ঝুড়ি বোঝাই গরুর গাড়ি বাজার থেকে টেনে আনে, আর আমাদের গাড়ি পাশে অপেক্ষা করে। একটি বিড়াল দেখে, ইঁদুরটি গরুর গাড়ির পেছনে ছুটে যায়।",
        },
        6: {
            "sentenceEn": "A girl carries garlic from the car into the kitchen. She wipes the basket with a rag, then opens the window for fresh air.",
            "sentenceBn": "একটি মেয়ে গাড়ি থেকে রসুন রান্নাঘরে নিয়ে যায়। সে ন্যাকড়া দিয়ে ঝুড়িটি মুছে তাজা বাতাসের জন্য জানালা খোলে।",
        },
        7: {
            "sentenceEn": "At the village fair, Rafi buys ginger and an egg for home. His sister holds a pony's rein, and her ring flashes as she gives him a grin.",
            "sentenceBn": "গ্রামের মেলায় রাফি বাড়ির জন্য আদা ও একটি ডিম কেনে। তার বোন টাট্টুঘোড়ার লাগাম ধরে, আর দাঁত বের করে হাসলে তার আংটি ঝিলমিল করে।",
        },
        8: {
            "sentenceEn": "The potato stew bubbles in a pot. The lid on top is too hot to touch, so Mina gives the waiting cat a gentle pat.",
            "sentenceBn": "হাঁড়িতে আলুর তরকারি ফুটছে। ওপরের ঢাকনাটি ছোঁয়ার জন্য অতিরিক্ত গরম, তাই মিনা অপেক্ষায় থাকা বিড়ালটিকে আলতো চাপড় দেয়।",
        },
        9: {
            "sentenceEn": "Rafi had soil in his hair after he pulled a radish. His sister has a basket ready for it.",
            "sentenceBn": "মূলা তোলার পর রাফির চুলে মাটি লেগেছিল। সেটি রাখার জন্য তার বোনের কাছে একটি ঝুড়ি আছে।",
        },
        10: {
            "sentenceEn": "On our garden trip, Mina finds a turnip. She tells her brother to put it in the basket, not to run, and to turn back toward the gate.",
            "sentenceBn": "আমাদের বাগান ভ্রমণে মিনা একটি শালগম খুঁজে পায়। সে তার ভাইকে সেটি ঝুড়িতে রাখতে, না দৌড়াতে এবং ফটকের দিকে ফিরে যেতে বলে।",
        },
    },
    "trees_plants": {
        1: {
            "sentenceEn": "Nila plants a tree with her aunt. Three green leaves open in the warm sun.",
            "sentenceBn": "নীলা তার খালার সঙ্গে একটি গাছ লাগায়। উষ্ণ রোদে তিনটি সবুজ পাতা মেলে ধরে।",
        },
        2: {
            "sentenceEn": "Lima's herb garden starts with mint in a pot. Her grandmother picks a few fragrant leaves and adds them to lunch.",
            "sentenceBn": "একটি টবে পুদিনা লাগিয়ে লিমার ভেষজ বাগান শুরু হয়। তার দাদি কয়েকটি সুগন্ধি পাতা তুলে দুপুরের খাবারে দেন।",
        },
        3: {
            "sentenceEn": "This root needs air too. Too much water can make it rot, so Rafi lets the soil dry.",
            "sentenceBn": "এই শিকড়টিরও বাতাস দরকার। অতিরিক্ত পানি দিলে এটি পচে যেতে পারে, তাই রাফি মাটি শুকাতে দেয়।",
        },
        4: {
            "sentenceEn": "Rafi and his pal follow a map to a tall palm. They sit in its shade with the map open on Rafi's lap.",
            "sentenceBn": "রাফি ও তার বন্ধু মানচিত্র দেখে একটি উঁচু তালগাছের কাছে যায়। তারা ছায়ায় বসে, আর মানচিত্রটি রাফির কোলে খোলা থাকে।",
        },
        5: {
            "sentenceEn": "Two men rest beneath a neem tree after work. They thank the leafy tree for its cool shade.",
            "sentenceBn": "কাজের পর দুজন পুরুষ একটি নিমগাছের নিচে বিশ্রাম নেন। শীতল ছায়ার জন্য তাঁরা পাতাভরা গাছটির প্রতি কৃতজ্ঞ হন।",
        },
        6: {
            "sentenceEn": "We ate rice beneath a teak tree, then drank warm tea. Grandma invited us to eat a banana and take one for the walk home.",
            "sentenceBn": "আমরা সেগুনগাছের নিচে ভাত খেয়েছিলাম, তারপর গরম চা পান করেছিলাম। দাদি আমাদের একটি কলা খেতে এবং বাড়ি ফেরার পথে আরেকটি সঙ্গে নিতে বলেছিলেন।",
        },
        7: {
            "sentenceEn": "A fern brushes Mira's finger beside the pond. She eats a fig and turns the ring on her finger while a fish lifts its fin.",
            "sentenceBn": "পুকুরের ধারে একটি ফার্ন মীরার আঙুল ছুঁয়ে যায়। সে একটি ডুমুর খায় ও আঙুলের আংটি ঘোরায়, আর একটি মাছ তার পাখনা তোলে।",
        },
        8: {
            "sentenceEn": "From the bus, Sami sees a sparrow dart into a bush. He watches the leafy hiding place until the bird peeks out.",
            "sentenceBn": "বাস থেকে সামি দেখে, একটি চড়ুই ঝোপের ভেতর ঢুকে পড়ছে। পাখিটি উঁকি দেওয়া পর্যন্ত সে পাতাভরা লুকানোর জায়গাটির দিকে তাকিয়ে থাকে।",
        },
        9: {
            "sentenceEn": "Under a picnic shelter, Auntie sets a small gas stove beside the grass for tea. She uses a rag to wipe the kettle's handle clean.",
            "sentenceBn": "বনভোজনের ছাউনির নিচে খালা চা বানাতে ঘাসের পাশে ছোট গ্যাসের চুলা রাখেন। তিনি ন্যাকড়া দিয়ে কেটলির হাতল পরিষ্কার করেন।",
        },
        10: {
            "sentenceEn": "Children run along the garden path and turn beside a wide tree trunk. They find a nut beside a tall clay urn filled with flowers.",
            "sentenceBn": "শিশুরা বাগানের পথ ধরে দৌড়ায় এবং গাছের চওড়া গুঁড়ির পাশে মোড় নেয়। তারা ফুলভরা একটি উঁচু মাটির কলসির পাশে বাদাম খুঁজে পায়।",
        },
    },
    "body_parts": {
        1: {
            "sentenceEn": "Mina stands outside the fence and points one arm toward a ram in its pen. From a safe distance, she watches the male sheep eat grass.",
            "sentenceBn": "মিনা বেড়ার বাইরে দাঁড়িয়ে খোঁয়াড়ের একটি পুরুষ ভেড়ার দিকে এক বাহু তুলে দেখায়। নিরাপদ দূরত্ব থেকে সে ভেড়াটিকে ঘাস খেতে দেখে।",
        },
        2: {
            "sentenceEn": "We are quiet while Rupa holds a shell to her ear. She smiles at the soft sound.",
            "sentenceBn": "রূপা কানে একটি ঝিনুক ধরে রাখলে আমরা চুপ থাকি। মৃদু শব্দ শুনে সে হাসে।",
        },
        3: {
            "sentenceEn": "Every child covers one eye during the vision game. The chart looks very clear to Tuhin.",
            "sentenceBn": "দৃষ্টির খেলায় প্রতিটি শিশু একটি চোখ ঢাকে। চার্টটি তুহিনের কাছে খুব স্পষ্ট দেখায়।",
        },
        4: {
            "sentenceEn": "A sudden gale bends the trees, so Mina stays indoors and rests her leg. Her teacher says people of every age should wait for strong wind to calm before walking outside.",
            "sentenceBn": "হঠাৎ ঝোড়ো হাওয়ায় গাছগুলো বাঁকে, তাই মিনা ঘরের ভেতরে থেকে তার পা বিশ্রামে রাখে। তার শিক্ষক বলেন, সব বয়সের মানুষেরই বাইরে হাঁটার আগে প্রবল বাতাস শান্ত হওয়া পর্যন্ত অপেক্ষা করা উচিত।",
        },
        5: {
            "sentenceEn": "Rafi takes a sip of water and wipes a drop from his lip. He walks slowly on the wet floor so he will not slip.",
            "sentenceBn": "রাফি পানিতে ছোট্ট চুমুক দেয় এবং ঠোঁটের ফোঁটা মুছে ফেলে। পা যেন পিছলে না যায়, তাই সে ভেজা মেঝেতে ধীরে হাঁটে।",
        },
        6: {
            "sentenceEn": "After a long cab ride, Mina gently stretches her back. The taxi driver stops at her family's gate, and she thanks him.",
            "sentenceBn": "দীর্ঘ সময় ট্যাক্সিতে চড়ার পর মিনা আলতো করে তার পিঠ সোজা করে। ট্যাক্সিচালক তার পরিবারের ফটকে গাড়ি থামালে সে তাঁকে ধন্যবাদ দেয়।",
        },
        7: {
            "sentenceEn": "A nice farmer treats every chicken and hen kindly in the farmyard. Rina lifts her chin and turns her neck to watch him feed them.",
            "sentenceBn": "খামারের ভালো কৃষকটি প্রতিটি মুরগি ও মাদি মুরগির সঙ্গে দয়ালু আচরণ করেন। রিনা চিবুক উঁচু করে ঘাড় ঘুরিয়ে তাঁকে পাখিগুলোকে খাবার দিতে দেখে।",
        },
        8: {
            "sentenceEn": "Fresh air lifts Tuli's hair as she walks outside. She ties it gently with a ribbon.",
            "sentenceBn": "বাইরে হাঁটার সময় তাজা বাতাসে তুলির চুল ওড়ে। সে ফিতা দিয়ে চুল আলতো করে বাঁধে।",
        },
        9: {
            "sentenceEn": "Rafi holds his mother's hand and walks beside her. He had brought her bag from home.",
            "sentenceBn": "রাফি তার মায়ের হাত ধরে পাশে পাশে হাঁটে। সে মায়ের ব্যাগটি বাড়ি থেকে এনেছিল।",
        },
        10: {
            "sentenceEn": "A father hides one orange in a bag and plays a smelling game with his son. The boy closes his eyes and uses his nose to find it.",
            "sentenceBn": "একজন বাবা একটি কমলা ব্যাগে লুকিয়ে তাঁর ছেলের সঙ্গে গন্ধ চেনার খেলা খেলেন। ছেলেটি চোখ বন্ধ করে নাক দিয়ে সেটি খুঁজে পায়।",
        },
    },
    "colors": {
        1: {
            "sentenceEn": "A deer stands beside a red hibiscus in the forest picture. The bright flower makes the gentle animal easy to spot.",
            "sentenceBn": "বনের ছবিতে একটি হরিণ লাল জবাফুলের পাশে দাঁড়িয়ে আছে। উজ্জ্বল ফুলটির পাশে শান্ত প্রাণীটিকে সহজে দেখা যায়।",
        },
        2: {
            "sentenceEn": "An old dog wears a collar the color of gold. He rests beside his owner in the warm sun.",
            "sentenceBn": "একটি বয়স্ক কুকুর সোনালি রঙের কলার পরে আছে। সে উষ্ণ রোদে মালিকের পাশে বিশ্রাম নেয়।",
        },
        3: {
            "sentenceEn": "Mita fills her pen with pink ink. Her teacher uses a pin to hold the drawing on the board.",
            "sentenceBn": "মিতা তার কলমে গোলাপি কালি ভরে। তার শিক্ষক ছবিটি বোর্ডে আটকাতে একটি পিন ব্যবহার করেন।",
        },
        4: {
            "sentenceEn": "A black cab waits outside the science lab. Inside, Rafi holds his hands behind his back while the teacher starts an experiment.",
            "sentenceBn": "বিজ্ঞান গবেষণাগারের বাইরে একটি কালো ট্যাক্সি অপেক্ষা করে। ভেতরে শিক্ষক পরীক্ষা শুরু করলে রাফি হাত দুটি পিঠের পেছনে রাখে।",
        },
        5: {
            "sentenceEn": "Now Iqbal places his own brown boat in a row of models. It won the class prize yesterday.",
            "sentenceBn": "এখন ইকবাল নিজের বাদামি নৌকাটি মডেলের সারিতে রাখে। সেটি গতকাল শ্রেণির পুরস্কার জিতেছে।",
        },
        6: {
            "sentenceEn": "The white ball is wet after rolling through a puddle. Rafi uses a bat to hit the ball while the coach stands with him.",
            "sentenceBn": "কাদাপানির ভেতর গড়ানোর পর সাদা বলটি ভেজা। প্রশিক্ষক পাশে থাকলে রাফি ব্যাট দিয়ে বলটিতে আঘাত করে।",
        },
        7: {
            "sentenceEn": "A big, bright kite moves to the right in a strong wind. One bit of its tail comes loose after a branch hit it.",
            "sentenceBn": "প্রবল বাতাসে একটি বড় উজ্জ্বল ঘুড়ি ডান দিকে যায়। ডালের আঘাতে তার লেজের ছোট্ট একটি টুকরো খুলে যায়।",
        },
        8: {
            "sentenceEn": "One runner in an orange cap ran past the gate. We said, 'We are proud of you,' in calm voices, and he turned one ear toward us.",
            "sentenceBn": "কমলা টুপি পরা একজন দৌড়বিদ ফটক পেরিয়ে দৌড়েছিল। আমরা শান্ত স্বরে বলেছিলাম, ‘আমরা তোমাকে নিয়ে গর্বিত,’ আর সে এক কান আমাদের দিকে ফিরিয়েছিল।",
        },
        9: {
            "sentenceEn": "A pup in a purple scarf drinks pure water after the walk. Its owner follows the park rule and keeps it on a lead.",
            "sentenceBn": "হাঁটার পর বেগুনি স্কার্ফ পরা একটি কুকুরছানা বিশুদ্ধ পানি পান করে। তার মালিক উদ্যানের নিয়ম মেনে তাকে দড়িতে রাখেন।",
        },
        10: {
            "sentenceEn": "Silver fish live in a pond below a hill. We watch the moon rise behind a veil of cloud.",
            "sentenceBn": "পাহাড়ের নিচের পুকুরে রুপালি মাছ বাস করে। আমরা মেঘের পাতলা ঘোমটার আড়াল থেকে চাঁদকে ওপরে উঠতে দেখি।",
        },
    },
    "family": {
        1: {
            "sentenceEn": "Uncle's son calls us from a country where snow is falling. He says he can build a snowman now.",
            "sentenceBn": "চাচার ছেলে এমন একটি দেশ থেকে আমাদের ফোন করে, যেখানে তুষার পড়ছে। সে বলে, এখন সে তুষারমানব বানাতে পারে।",
        },
        2: {
            "sentenceEn": "My aunt packs a tuna sandwich for our picnic. She smiles when an ant tries to carry away a crumb.",
            "sentenceBn": "আমার খালা বনভোজনের জন্য টুনা মাছের স্যান্ডউইচ রাখেন। একটি পিঁপড়া খাবারের কণা নিয়ে যেতে চাইলে তিনি হাসেন।",
        },
        3: {
            "sentenceEn": "A girl brings garlic home in the family car. She uses a rag to wipe the muddy basket.",
            "sentenceBn": "একটি মেয়ে পরিবারের গাড়িতে রসুন বাড়ি আনে। সে ন্যাকড়া দিয়ে কাদামাখা ঝুড়িটি মোছে।",
        },
        4: {
            "sentenceEn": "A child hid behind the pantry door during a game. His giggle came from beside a jar with a loose lid.",
            "sentenceBn": "খেলার সময় একটি শিশু খাবার রাখার ঘরের দরজার পেছনে লুকিয়েছিল। ঢিলা ঢাকনাওয়ালা বয়ামের পাশ থেকে তার হাসির শব্দ আসে।",
        },
        5: {
            "sentenceEn": "Our elder cousin led us along a red path at the nature park. We saw an eel in the pond and a deer near the trees.",
            "sentenceBn": "আমাদের বড় চাচাতো ভাই প্রকৃতি উদ্যানের লাল পথ ধরে আমাদের নিয়ে গিয়েছিল। আমরা পুকুরে একটি বাইম মাছ ও গাছের কাছে একটি হরিণ দেখেছি।",
        },
        6: {
            "sentenceEn": "The twins sit beside a tin of biscuits. They open its lid, share the snacks, and hope to win the family quiz.",
            "sentenceBn": "যমজ শিশু দুটি বিস্কুটের টিনের পাশে বসে। তারা টিনটির ঢাকনা খুলে নাশতা ভাগ করে খায় এবং পারিবারিক প্রশ্নোত্তর খেলায় জিততে চায়।",
        },
        7: {
            "sentenceEn": "Our family shares roasted yam at a picnic. Mina asks if she may fly her kite after she helps lay the plates on the mat.",
            "sentenceBn": "আমাদের পরিবার বনভোজনে পোড়ানো মিষ্টি আলু ভাগ করে খায়। মিনা জানতে চায়, মাদুরে থালাগুলো সাজিয়ে রাখতে সাহায্য করার পর সে ঘুড়ি ওড়াতে পারে কি না।",
        },
        8: {
            "sentenceEn": "My friend and I ride our bicycles along a red path. We stop by a fern to find a missing bell.",
            "sentenceBn": "আমার বন্ধু ও আমি লাল পথ ধরে সাইকেলে চড়ি। হারানো ঘণ্টাটি খুঁজতে আমরা একটি ফার্নগাছের পাশে থামি।",
        },
        9: {
            "sentenceEn": "My sister opens a set of story cards and asks me to sit beside her. A bird folds its wings on the window ledge while we rest.",
            "sentenceBn": "আমার বোন গল্পের তাসের একটি সেট খুলে আমাকে তার পাশে বসতে বলে। আমরা বিশ্রাম নেওয়ার সময় জানালার তাকে একটি পাখি নিজের ডানা গুটিয়ে রাখে।",
        },
        10: {
            "sentenceEn": "My brother makes hot tea with an herb from our mother's garden. She smiles because he picked the leaves from her plant. The warm drink smells fresh.",
            "sentenceBn": "আমার ভাই মায়ের বাগানের একটি ভেষজ দিয়ে গরম চা বানায়। সে তাঁর গাছ থেকে পাতা তুলেছে বলে মা হাসেন। উষ্ণ পানীয়টির গন্ধ সতেজ।",
        },
    },
}


_ASSET_PATH = Path(__file__).resolve().parents[1] / "app/src/main/assets/categories.json"
_ALL_CAPS_WORD_RE = re.compile(r"(?<![A-Za-z])[A-Z]{2,}(?![A-Za-z])")
_SENTENCE_END_RE = re.compile(r"[.!?](?=\s|$)")


def _whole_word_present(text: str, word: str) -> bool:
    return bool(
        re.search(
            rf"(?<![A-Za-z]){re.escape(word)}(?![A-Za-z])",
            text,
            flags=re.IGNORECASE,
        )
    )


def assert_editorial_copy(asset_path: Path = _ASSET_PATH) -> None:
    """Raise AssertionError when keys, coverage, or prose casing drifts."""

    with asset_path.open(encoding="utf-8") as asset_file:
        catalog: dict[str, Any] = json.load(asset_file)

    target_ids = set(CATEGORY_STORIES)
    assert set(LEVEL_STORIES) == target_ids, "category mapping keys differ"

    categories = {
        category["id"]: category
        for category in catalog["categories"]
        if category["id"] in target_ids
    }
    assert set(categories) == target_ids, "asset category keys differ"

    for category_id, category in categories.items():
        category_copy = CATEGORY_STORIES[category_id]
        assert set(category_copy) == {"storyEn", "storyBn"}
        story_en = category_copy["storyEn"]
        story_bn = category_copy["storyBn"]
        assert story_bn.strip(), f"{category_id}: empty Bengali story"
        for language, story in (("English", story_en), ("Bengali", story_bn)):
            paragraph_count = len(story.split("\n\n"))
            assert 2 <= paragraph_count <= 3, (
                f"{category_id}: expected 2-3 {language} paragraphs, got "
                f"{paragraph_count}"
            )
        assert not _ALL_CAPS_WORD_RE.search(story_en), (
            f"{category_id}: all-caps token in category story"
        )
        missing_story_words = [
            word
            for word in category["storyWords"]
            if not _whole_word_present(story_en, word)
        ]
        assert not missing_story_words, (
            f"{category_id}: missing category words {missing_story_words}"
        )

        asset_levels = {level["id"]: level for level in category["levels"]}
        level_copy = LEVEL_STORIES[category_id]
        assert set(level_copy) == set(asset_levels), (
            f"{category_id}: level keys differ from asset"
        )

        for level_id, level in asset_levels.items():
            copy = level_copy[level_id]
            assert set(copy) == {"sentenceEn", "sentenceBn"}
            sentence_en = copy["sentenceEn"]
            assert copy["sentenceBn"].strip(), (
                f"{category_id}/{level_id}: empty Bengali story"
            )
            assert not _ALL_CAPS_WORD_RE.search(sentence_en), (
                f"{category_id}/{level_id}: all-caps token in level story"
            )
            sentence_count = len(_SENTENCE_END_RE.findall(sentence_en))
            assert 2 <= sentence_count <= 4, (
                f"{category_id}/{level_id}: expected 2-4 sentences, got "
                f"{sentence_count}"
            )
            missing_level_words = [
                word
                for word in level["words"]
                if not _whole_word_present(sentence_en, word)
            ]
            assert not missing_level_words, (
                f"{category_id}/{level_id}: missing level words "
                f"{missing_level_words}"
            )


if __name__ == "__main__":
    assert_editorial_copy()
    print("Editorial life copy checks passed.")
