"""Editorial story copy for six everyday WordBloom categories.

Run this module directly to check the copy against the current category asset::

    python3 tools/editorial_daily_copy.py

The checks cover structure, whole-word English coverage, sentence counts for
level stories, and accidental all-uppercase word stuffing. Bengali copy still
requires review by a Bangladeshi Bengali language reviewer and primary educator
before release.
"""

from __future__ import annotations

import json
import re
from pathlib import Path


CATEGORY_STORIES = {
    "school": {
        "storyEn": (
            "On one bright school day, Rafi carries a bag and wears a cap while "
            "his class plans a camp. The classroom door is open, and their teacher "
            "spreads a map beside a globe and draws a line from the road to the river "
            "with a pen. She writes the camp rules on the board and says, \"Please "
            "grab a chair and place this set of map cards on your paper.\" A bad ink "
            "mark lands near a pea and a pear from lunch, so Rafi wipes the table with "
            "a rag while Mina uses an eraser on her pencil work.\n\nFor the school "
            "exhibition, a rich visitor arrives by car and walks among the learning "
            "stations. At the nature station, fresh air lifts her hair as she rests one "
            "leg beside a log. At the river station, the children see a model boat with "
            "an oar and a fishing rod. At the listening station, they are careful to use "
            "each ear, which helps ease their worry about the test. At the reading "
            "station, the teacher gives them a nice smile. Rafi presses a finger to his "
            "lip for quiet, and the lesson begins."
        ),
        "storyBn": (
            "এক উজ্জ্বল সকালে রাফি ব্যাগ নিয়ে ও ক্যাপ পরে স্কুলে আসে; সেদিন তার "
            "শ্রেণি শিবিরে যাওয়ার পরিকল্পনা করছে। শ্রেণিকক্ষের দরজা খোলা, আর শিক্ষিকা "
            "গ্লোবের পাশে একটি মানচিত্র মেলে কলম দিয়ে রাস্তা থেকে নদী পর্যন্ত একটি "
            "রেখা আঁকেন। তিনি বোর্ডে শিবিরের নিয়ম লেখেন এবং বলেন, ‘একটি চেয়ার টেনে "
            "নাও, আর মানচিত্রের কার্ডের সেটটি কাগজের ওপর রাখো।’ দুপুরের খাবারের মটর "
            "ও নাশপাতির পাশে কলমের একটি খারাপ দাগ পড়লে রাফি ন্যাকড়া দিয়ে টেবিল মোছে, "
            "আর মিনা পেন্সিলের ভুল মুছতে রাবার ব্যবহার করে।\n\nস্কুলের প্রদর্শনী দেখতে "
            "এক ধনী অতিথি গাড়িতে করে এসে শেখার স্টেশনগুলো ঘুরে দেখেন। প্রকৃতি স্টেশনে "
            "নির্মল বাতাসে তাঁর চুল ওড়ে; তিনি গাছের গুঁড়ির পাশে এক পা বিশ্রাম দিয়ে "
            "দাঁড়ান। নদী স্টেশনে শিশুরা দাঁড় ও মাছ ধরার ছিপসহ নৌকার একটি মডেল দেখে। "
            "শোনার স্টেশনে তারা দুই কান দিয়ে মনোযোগ দেয়, আর এতে পরীক্ষার দুশ্চিন্তা "
            "কমে। পড়ার স্টেশনে শিক্ষিকা সুন্দর করে হাসেন। সবাইকে চুপ থাকতে বলে রাফি "
            "ঠোঁটে আঙুল রাখে, তারপর পাঠ শুরু হয়।"
        ),
    },
    "transport": {
        "storyEn": (
            "Once, our class rode a bus to the transport museum with a plan to compare "
            "vehicles. We saw a car, van, truck, train, plane, ship, boat, canoe, and a "
            "cart pulled by one calm horse while rain tapped the windows. A guide "
            "pulled a tab on his display drawer to show "
            "a sub moving underwater, while a roof vane turned in the wind.\n\nDuring "
            "lunch at the museum café, a pea fell from a pan of vegetables beside an "
            "oat biscuit, and an "
            "ant—not a rat—found it. A cat watched a bat model hanging above the hall. "
            "I took a sip of water, put my pen safely in my bag, rested a hand on my "
            "hip, and ran to join an act "
            "about road safety. In the craft room, we learned that anyone can cut paper "
            "to make a bright safety sign."
        ),
        "storyBn": (
            "একবার আমাদের শ্রেণি যানবাহন জাদুঘরে বাসে গেল; নানা যান তুলনা করাই ছিল "
            "আমাদের পরিকল্পনা। আমরা গাড়ি, ভ্যান, ট্রাক, ট্রেন, বিমান, জাহাজ, নৌকা, "
            "ডিঙি এবং একটি শান্ত ঘোড়ায় টানা গরুর গাড়ি দেখলাম; তখন জানালায় বৃষ্টির "
            "ফোঁটা পড়ছিল। একজন প্রদর্শক প্রদর্শনীর ড্রয়ারের ছোট ট্যাবটি টেনে পানির নিচে "
            "চলা সাবমেরিন দেখালেন, আর ছাদের বায়ুদিক নির্দেশকটি হাওয়ায় ঘুরছিল।\n\n"
            "জাদুঘরের খাবারের দোকানে একটি সবজির কড়াই থেকে মটর ওটসের বিস্কুটের পাশে "
            "পড়ে গেল; ইঁদুর নয়, একটি পিঁপড়া সেটি খুঁজে "
            "পেল। হলের ওপরে ঝুলে থাকা বাদুড়ের মডেলের দিকে একটি বিড়াল তাকিয়ে ছিল। "
            "আমি পানিতে চুমুক দিলাম, কলমটি ব্যাগে নিরাপদে রেখে কোমরে হাত দিলাম এবং "
            "সড়ক নিরাপত্তার অভিনয়ে "
            "যোগ দিতে দৌড়ালাম। কারুশিল্পের ঘরে আমরা উজ্জ্বল নিরাপত্তা-চিহ্ন বানাতে "
            "কাগজ কাটতে শিখলাম।"
        ),
    },
    "weather": {
        "storyEn": (
            "One day, a farmer and his son went into the yard to prepare for changing "
            "weather. The air was hot and dry, but both saw a dark cloud, and the wind "
            "began to rise. They ran inside as rain made the road wet and cooled the old "
            "tar. After the storm, a frog hopped out of the fog near a rat that sat by "
            "the shed.\n\nThe farmer said, \"Now we can sow ten new seeds, but first we "
            "must sort the ones that could rot.\" On their own weather chart, they used "
            "art to draw cold snow, a bright star, and cod swimming in a cold sea. Most "
            "of the page showed weather that their village knows, and they hoped their "
            "careful work would win a prize for the day."
        ),
        "storyBn": (
            "একদিন এক কৃষক ও তাঁর ছেলে বদলে যাওয়া আবহাওয়ার প্রস্তুতি নিতে উঠানে "
            "গেল। বাতাস ছিল গরম ও শুকনো, কিন্তু দুজনেই কালো মেঘ দেখল এবং হাওয়া জোরে "
            "বইতে শুরু করল। বৃষ্টিতে রাস্তা ভিজে পুরোনো আলকাতরা ঠান্ডা হয়ে গেলে তারা "
            "দৌড়ে ঘরে ঢুকল। ঝড়ের পর কুয়াশা থেকে একটি ব্যাঙ লাফিয়ে বেরোল; কাছেই চালার "
            "পাশে একটি ইঁদুর বসে ছিল।\n\nকৃষক বললেন, ‘এখন আমরা দশটি নতুন বীজ বপন করতে "
            "পারি, তবে আগে যেগুলো পচে যেতে পারে সেগুলো বাছাই করতে হবে।’ নিজেদের আবহাওয়া "
            "ছকে তারা ছবি এঁকে ঠান্ডা তুষার, উজ্জ্বল তারা এবং শীতল সাগরে সাঁতার-কাটা কড "
            "মাছ দেখাল। পাতার বেশির ভাগ অংশে তাদের গ্রামের পরিচিত আবহাওয়া ছিল, আর তারা "
            "আশা করল যত্নের কাজটি দিনের পুরস্কার জিতবে।"
        ),
    },
    "food_kitchen": {
        "storyEn": (
            "One rainy morning, a lad named Sami helped his grandmother cook dal in "
            "their village kitchen. He took a pot from a low shelf, set a pan on top of "
            "the stove, and peeled ginger while she cracked one egg from their hen. Her "
            "ring shone as she added meat and salt.\n\nSami placed a bowl on a mat and "
            "poured tea into a cup, then warmed bread with honey. At last, they sat "
            "together. Grandmother ate first and invited Sami to eat. After the meal, he "
            "wanted to read, then took a short nap while the dishes dried."
        ),
        "storyBn": (
            "এক বৃষ্টির সকালে সামি নামের এক ছেলে গ্রামের রান্নাঘরে দাদিকে ডাল রান্নায় "
            "সাহায্য করল। সে নিচু তাক থেকে হাঁড়ি নামিয়ে চুলার ওপর কড়াই বসাল এবং আদা "
            "ছাড়াল; দাদি তাঁদের মুরগির একটি ডিম ভাঙলেন। মাংস ও লবণ দেওয়ার সময় তাঁর "
            "আংটি চকচক করছিল।\n\nসামি মাদুরে একটি বাটি রাখল, কাপে চা ঢালল এবং মধু দিয়ে "
            "রুটি গরম করল। শেষে তারা একসঙ্গে বসল। দাদি আগে খেলেন এবং সামিকে খেতে "
            "ডাকলেন। খাবারের পর সামি বই পড়তে চাইল, তারপর বাসন শুকানোর সময় একটু ঘুমাল।"
        ),
    },
    "clothing": {
        "storyEn": (
            "What should Maya pack for the school costume day? She folds a shirt, "
            "pants, a vest, a coat, and a scarf, then adds a cap and a hat. She finds "
            "one shoe under the bed; the other is drying beside the car after a muddy "
            "trip. Her belt lies on "
            "a soft cloth, and this neat set will let her dress quickly.\n\nIt is hot, so "
            "she puts on sunscreen to avoid a tan and carries a lot of water. At school, "
            "she and a vet act in a play about caring for a cat. This is the vet's "
            "scene: his hose sprays a pretend garden, and its nozzle stays pointed far "
            "from the costumes. Maya gives the cat a gentle pat. After the show, the "
            "children sit together."
        ),
        "storyBn": (
            "স্কুলের সাজপোশাকের দিনে মায়া কী গুছিয়ে নেবে? সে শার্ট, প্যান্ট, ভেস্ট, "
            "কোট ও স্কার্ফ ভাঁজ করে, তারপর ক্যাপ ও টুপি রাখে। সে একটি জুতা বিছানার নিচে "
            "পায়; অন্যটি কাদামাখা ভ্রমণের পর গাড়ির পাশে শুকাচ্ছে। তার বেল্টটি নরম "
            "কাপড়ের ওপর পড়ে আছে; "
            "এই গোছানো সেট তাকে দ্রুত পোশাক পরতে দেবে।\n\nবাইরে গরম, তাই রোদে ত্বক "
            "তামাটে হওয়া এড়াতে সে সানস্ক্রিন লাগায় এবং অনেক পানি নেয়। স্কুলে মায়া "
            "ও একজন পশু চিকিৎসক মিলে বিড়ালের যত্ন নিয়ে নাটকে অভিনয় করে। এটি পশু "
            "চিকিৎসকের দৃশ্য: তাঁর পানির নলটি সাজানো বাগানে পানি ছিটায়, আর নলটির মুখ "
            "পোশাক থেকে দূরে রাখা হয়। মায়া বিড়ালটিকে আলতো চাপড় দেয়। অনুষ্ঠান শেষে "
            "শিশুরা একসঙ্গে বসে।"
        ),
    },
    "numbers": {
        "storyEn": (
            "At the town math fair, three friends help a woman at her counting stall. "
            "She points over there to one tree, where the clean air carries a myna's "
            "song. The "
            "children tie two ribbons to a net, place four fur puppets beside a yam, "
            "and count eight coins, ten pencils, and a dozen shells.\n\nA man asks for a "
            "pair of puppets. The friends get two clean labels and do not rip them as "
            "they tie them on. One card shows zero, and another shows ore "
            "from a mine; a third shows a fox in its den. Many visitors stop at the "
            "stall. \"How many objects may go in "
            "each box?\" asks the woman. At the end, our team plays a game: throw the "
            "soft ball and hit the number named. When every number has a turn, the work "
            "is done."
        ),
        "storyBn": (
            "শহরের গণিত মেলায় তিন বন্ধু এক নারীর গোনার দোকানে তাঁকে সাহায্য করে। "
            "তিনি সেখানে একটি গাছ দেখিয়ে দেন, যেখানকার নির্মল বাতাসে শালিকের ডাক ভেসে "
            "আসে। শিশুরা একটি "
            "জালে দুটি ফিতা বাঁধে, একটি মিষ্টি আলুর পাশে চারটি পশমের পুতুল রাখে এবং "
            "আটটি মুদ্রা, দশটি পেন্সিল ও এক ডজন ঝিনুক গোনে।\n\nএক ব্যক্তি এক জোড়া পুতুল "
            "চান। বন্ধুরা দুটি পরিষ্কার লেবেল পায় এবং সেগুলো বাঁধার সময় ছিঁড়ে ফেলে না। "
            "একটি কার্ডে শূন্য, আরেকটিতে খনি থেকে পাওয়া ধাতব "
            "আকরিক; তৃতীয়টিতে গর্তের ভেতর একটি শিয়াল। অনেক দর্শক দোকানটিতে থামে। "
            "নারীটি জিজ্ঞেস করেন, ‘প্রতিটি "
            "বাক্সে কতটি জিনিস রাখা যেতে পারে?’ শেষে আমাদের দল একটি খেলা খেলে: নরম বল "
            "ছুড়ে বলা সংখ্যাটিতে আঘাত করতে হবে। প্রতিটি সংখ্যার পালা হলে কাজ শেষ।"
        ),
    },
}


LEVEL_STORIES = {
    "school": {
        1: {
            "sentenceEn": (
                "Mitu hangs her bag beside the classroom door. She uses a rag to wipe "
                "the table, then tells Rafi to grab the dustpan."
            ),
            "sentenceBn": (
                "মিতু শ্রেণিকক্ষের দরজার পাশে তার ব্যাগ ঝুলিয়ে রাখে। সে ন্যাকড়া দিয়ে "
                "টেবিল মোছে, তারপর রাফিকে ময়লা তোলার পাত্রটি তাড়াতাড়ি হাতে নিতে বলে।"
            ),
        },
        2: {
            "sentenceEn": (
                "Before the scout camp, Rafi studies a map of the park. He puts on his "
                "cap and marks the meeting place."
            ),
            "sentenceBn": (
                "স্কাউট শিবিরে যাওয়ার আগে রাফি পার্কের মানচিত্র দেখে। সে ক্যাপ পরে "
                "মিলিত হওয়ার জায়গাটি চিহ্নিত করে।"
            ),
        },
        3: {
            "sentenceEn": (
                "The teacher asks Mina to open her notebook. She carefully writes one "
                "sentence with a blue pen."
            ),
            "sentenceBn": (
                "শিক্ষিকা মিনাকে খাতা খুলতে বলেন। সে নীল কলম দিয়ে যত্ন করে একটি বাক্য "
                "লেখে।"
            ),
        },
        4: {
            "sentenceEn": (
                "Our teacher gives each group a set of number cards. We use them to "
                "prepare for Friday's test."
            ),
            "sentenceBn": (
                "শিক্ষিকা প্রতিটি দলকে সংখ্যার কার্ডের একটি সেট দেন। শুক্রবারের পরীক্ষার "
                "প্রস্তুতিতে আমরা সেগুলো ব্যবহার করি।"
            ),
        },
        5: {
            "sentenceEn": (
                "The teacher draws a road on the board and marks a bad place to cross. "
                "Beside her, a fishing rod and an oar show how long objects should be "
                "carried safely."
            ),
            "sentenceBn": (
                "শিক্ষিকা বোর্ডে একটি রাস্তা এঁকে পারাপারের একটি খারাপ জায়গা চিহ্নিত "
                "করেন। তাঁর পাশে থাকা মাছ ধরার ছিপ ও নৌকার দাঁড় দিয়ে তিনি দেখান লম্বা "
                "জিনিস কীভাবে নিরাপদে বহন করতে হয়।"
            ),
        },
        6: {
            "sentenceEn": (
                "A rich man arrives by car to donate books to the school. He sits on a "
                "chair as fresh air lifts his hair, and he tells the children that "
                "sharing matters more than money."
            ),
            "sentenceBn": (
                "এক ধনী ব্যক্তি স্কুলে বই দিতে গাড়িতে করে আসেন। তিনি চেয়ারে বসলে নির্মল "
                "বাতাসে তাঁর চুল ওড়ে, আর তিনি শিশুদের বলেন যে টাকার চেয়ে ভাগ করে নেওয়া "
                "বেশি মূল্যবান।"
            ),
        },
        7: {
            "sentenceEn": (
                "During nature class, Tuli points to a forest on the globe. Then she "
                "balances on one leg beside a fallen log in the school garden."
            ),
            "sentenceBn": (
                "প্রকৃতি ক্লাসে তুলি গ্লোবে একটি বন দেখায়। তারপর সে স্কুলের বাগানে পড়ে "
                "থাকা গাছের গুঁড়ির পাশে এক পায়ে ভারসাম্য রাখে।"
            ),
        },
        8: {
            "sentenceEn": (
                "A pea and a slice of pear are on Rafi's lunch plate. He holds a paper "
                "to one ear and laughs at its rustling sound."
            ),
            "sentenceBn": (
                "রাফির দুপুরের খাবারের প্লেটে একটি মটর ও এক টুকরা নাশপাতি আছে। সে এক "
                "কানের কাছে কাগজ ধরে তার খসখস শব্দ শুনে হাসে।"
            ),
        },
        9: {
            "sentenceEn": (
                "Rupa can see that her sums are wrong, but her teacher speaks gently to "
                "ease her worry. She tucks her hair behind one ear and uses an eraser "
                "to try again."
            ),
            "sentenceBn": (
                "রূপা দেখতে পায় যে তার অঙ্কগুলো ভুল হয়েছে, কিন্তু শিক্ষিকা নরম করে "
                "কথা বলে তার দুশ্চিন্তা কমান। সে এক কানের পেছনে চুল গুঁজে রাবার দিয়ে "
                "মুছে আবার চেষ্টা করে।"
            ),
        },
        10: {
            "sentenceEn": (
                "A nice teacher shows Imran how to draw a straight line with a pencil. "
                "He puts down his pen, bites his lip in thought, and tries it himself."
            ),
            "sentenceBn": (
                "একজন সদয় শিক্ষক ইমরানকে পেন্সিল দিয়ে সোজা রেখা আঁকা দেখান। ইমরান "
                "কলম নামিয়ে রেখে ভাবতে ভাবতে ঠোঁট কামড়ায়, তারপর নিজে চেষ্টা করে।"
            ),
        },
    },
    "transport": {
        1: {
            "sentenceEn": (
                "A bus carries our class to the science museum. Inside, we see a model "
                "of a sub that can travel under water."
            ),
            "sentenceBn": (
                "একটি বাস আমাদের শ্রেণিকে বিজ্ঞান জাদুঘরে নিয়ে যায়। ভেতরে আমরা এমন "
                "একটি সাবমেরিনের মডেল দেখি, যা পানির নিচে চলতে পারে।"
            ),
        },
        2: {
            "sentenceEn": (
                "A farmer guides his ox cart along the village road. A cat watches from "
                "the gate as a car passes slowly."
            ),
            "sentenceBn": (
                "এক কৃষক গ্রামের রাস্তা ধরে তাঁর গরুর গাড়ি চালান। একটি মোটরগাড়ি ধীরে "
                "চলে যাওয়ার সময় ফটক থেকে বিড়াল তাকিয়ে থাকে।"
            ),
        },
        3: {
            "sentenceEn": (
                "The delivery van stops beside our school. Above it, the weather vane "
                "points east in the wind."
            ),
            "sentenceBn": (
                "মালবাহী ভ্যানটি আমাদের স্কুলের পাশে থামে। তার ওপরে বায়ুদিক নির্দেশকটি "
                "হাওয়ায় পূর্ব দিকে ঘুরে থাকে।"
            ),
        },
        4: {
            "sentenceEn": (
                "On the boat, Sami eats an oat biscuit and watches a bat fly over the "
                "river at dusk. He pulls the tab on his juice box, drinks the juice, "
                "and then puts the empty pack away."
            ),
            "sentenceBn": (
                "নৌকায় বসে সামি ওটসের বিস্কুট খায় এবং সন্ধ্যায় নদীর ওপর দিয়ে একটি "
                "বাদুড় উড়তে দেখে। সে জুসের বাক্সের ছোট ট্যাবটি টেনে খোলে, জুস পান করে, "
                "তারপর খালি প্যাকেটটি সরিয়ে রাখে।"
            ),
        },
        5: {
            "sentenceEn": (
                "At the village fair, an ox pulls a cart past a parked car. Onstage, a "
                "cat chases a pretend rat, and their funny act makes everyone laugh."
            ),
            "sentenceBn": (
                "গ্রামের মেলায় একটি বলদ গরুর গাড়ি টেনে দাঁড়ানো মোটরগাড়ির পাশ দিয়ে "
                "যায়। মঞ্চে একটি বিড়াল নকল ইঁদুরকে তাড়া করলে তাদের মজার অভিনয়ে সবাই "
                "হাসে।"
            ),
        },
        6: {
            "sentenceEn": (
                "Arif stands on the ship's deck with his hand on his hip. He takes a "
                "sip of water while the crew checks the ropes."
            ),
            "sentenceBn": (
                "আরিফ জাহাজের ডেকে কোমরে হাত রেখে দাঁড়ায়। নাবিকেরা দড়ি পরীক্ষা করার "
                "সময় সে পানিতে একটু চুমুক দেয়।"
            ),
        },
        7: {
            "sentenceEn": (
                "Once, Lila saw a canoe beside the lake. One guide showed her how a "
                "paddle can move the little boat."
            ),
            "sentenceBn": (
                "একবার লীলা হ্রদের পাশে একটি ডিঙি নৌকা দেখেছিল। একজন পথপ্রদর্শক তাকে "
                "দেখিয়েছিলেন যে বৈঠা দিয়ে ছোট নৌকাটি চালানো যায়।"
            ),
        },
        8: {
            "sentenceEn": (
                "Heavy rain began as our train reached the station. An ant hid under a "
                "leaf, a rat ran beside the tracks, and we stayed safely on the platform."
            ),
            "sentenceBn": (
                "আমাদের ট্রেন স্টেশনে পৌঁছাতেই ভারী বৃষ্টি শুরু হলো। একটি পিঁপড়া পাতার "
                "নিচে লুকাল, একটি ইঁদুর রেললাইনের পাশে দৌড়াল, আর আমরা নিরাপদে প্ল্যাটফর্মে "
                "রইলাম।"
            ),
        },
        9: {
            "sentenceEn": (
                "Before their trip, Nila uses a pen to draw a plane above their travel "
                "plan. In the kitchen, Dad drops one pea into a pan of vegetables for "
                "lunch."
            ),
            "sentenceBn": (
                "ভ্রমণের আগে নীলা কলম দিয়ে তাদের ভ্রমণ-পরিকল্পনার ওপরে একটি বিমান আঁকে। "
                "রান্নাঘরে বাবা দুপুরের সবজির কড়াইয়ে একটি মটর দেন।"
            ),
        },
        10: {
            "sentenceEn": (
                "A truck brings cardboard to the recycling centre. Workers cut the "
                "sheets into smaller pieces for new boxes."
            ),
            "sentenceBn": (
                "একটি ট্রাক পুনর্ব্যবহার কেন্দ্রে পিচবোর্ড নিয়ে আসে। কর্মীরা নতুন বাক্স "
                "বানাতে পাতাগুলো ছোট টুকরায় কাটেন।"
            ),
        },
    },
    "weather": {
        1: {
            "sentenceEn": (
                "After a sunny day, the clothes are dry. Rafi brings them in from the "
                "yard before evening."
            ),
            "sentenceBn": (
                "রৌদ্রোজ্জ্বল দিনের পর কাপড়গুলো শুকনো হয়েছে। সন্ধ্যার আগে রাফি উঠান "
                "থেকে সেগুলো ঘরে আনে।"
            ),
        },
        2: {
            "sentenceEn": (
                "Thick fog covers the pond in the morning. A frog waits on a leaf for "
                "the sun to warm the air."
            ),
            "sentenceBn": (
                "সকালে ঘন কুয়াশায় পুকুর ঢেকে যায়। বাতাস গরম করার জন্য সূর্যের অপেক্ষায় "
                "একটি ব্যাঙ পাতার ওপর বসে থাকে।"
            ),
        },
        3: {
            "sentenceEn": (
                "The afternoon is hot, so Nila and Rafi sit in the shade. Both drink "
                "water to stay cool."
            ),
            "sentenceBn": (
                "দুপুরটি গরম, তাই নীলা ও রাফি ছায়ায় বসে। ঠান্ডা থাকতে দুজনেই পানি পান "
                "করে।"
            ),
        },
        4: {
            "sentenceEn": (
                "Mina went outside in her new raincoat. She counted ten wet footprints "
                "on the veranda."
            ),
            "sentenceBn": (
                "মিনা তার নতুন বর্ষাতি পরে বাইরে গেল। সে বারান্দায় দশটি ভেজা পায়ের ছাপ "
                "গুনল।"
            ),
        },
        5: {
            "sentenceEn": (
                "An old fisherman saw a grey cloud over the cold sea. He knew the "
                "weather could change, so he brought his cod catch safely home."
            ),
            "sentenceBn": (
                "এক বৃদ্ধ জেলে ঠান্ডা সাগরের ওপর ধূসর মেঘ দেখলেন। তিনি জানতেন আবহাওয়া "
                "বদলাতে পারে, তাই ধরা কড মাছ নিরাপদে বাড়ি নিয়ে এলেন।"
            ),
        },
        6: {
            "sentenceEn": (
                "Rain swept across the field and cooled the air. We ran to the school "
                "veranda and watched from a dry place."
            ),
            "sentenceBn": (
                "মাঠজুড়ে বৃষ্টি নেমে বাতাস ঠান্ডা করল। আমরা স্কুলের বারান্দায় দৌড়ে গিয়ে "
                "শুকনো জায়গা থেকে বৃষ্টি দেখলাম।"
            ),
        },
        7: {
            "sentenceEn": (
                "A farmer and his son look at a book about snow, which they have never "
                "seen in their own village. Now they sow winter seeds in the cool soil."
            ),
            "sentenceBn": (
                "এক কৃষক ও তাঁর ছেলে তুষার নিয়ে একটি বই দেখে; নিজেদের গ্রামে তারা কখনো "
                "তুষার দেখেনি। এখন তারা ঠান্ডা মাটিতে শীতের বীজ বপন করে।"
            ),
        },
        8: {
            "sentenceEn": (
                "At night, Rima sat by the window and painted a bright star for her art "
                "project. Outside, a rat hurried across the dark tar road."
            ),
            "sentenceBn": (
                "রাতে রিমা জানালার পাশে বসে তার শিল্প প্রকল্পের জন্য একটি উজ্জ্বল তারা "
                "আঁকল। বাইরে একটি ইঁদুর কালো পিচঢালা রাস্তা তাড়াতাড়ি পার হলো।"
            ),
        },
        9: {
            "sentenceEn": (
                "The wind lifts every kite above the field. Rafi hopes his steady kite "
                "will win the friendly contest."
            ),
            "sentenceBn": (
                "হাওয়া প্রতিটি ঘুড়িকে মাঠের ওপরে তোলে। রাফি আশা করে তার স্থির ঘুড়িটি "
                "বন্ধুত্বপূর্ণ প্রতিযোগিতায় জিতবে।"
            ),
        },
        10: {
            "sentenceEn": (
                "After the storm, most mangoes are still fresh, but a few may rot. The "
                "family helps sort the good fruit into baskets."
            ),
            "sentenceBn": (
                "ঝড়ের পর বেশির ভাগ আম এখনো টাটকা, তবে কয়েকটি পচে যেতে পারে। পরিবারের "
                "সবাই ভালো ফল বাছাই করে ঝুড়িতে রাখতে সাহায্য করে।"
            ),
        },
    },
    "food_kitchen": {
        1: {
            "sentenceEn": (
                "A helpful lad brings warm dal to the table. He waits until it cools, "
                "then tastes a spoonful."
            ),
            "sentenceBn": (
                "এক সাহায্যকারী ছেলে টেবিলে গরম ডাল নিয়ে আসে। ডাল ঠান্ডা হলে সে এক "
                "চামচ চেখে দেখে।"
            ),
        },
        2: {
            "sentenceEn": (
                "Mother grates ginger into the egg curry. Her silver ring shines as "
                "she stirs the pot."
            ),
            "sentenceBn": (
                "মা ডিমের তরকারিতে আদা কুচি করেন। হাঁড়ি নাড়ার সময় তাঁর রুপার আংটি "
                "চকচক করে।"
            ),
        },
        3: {
            "sentenceEn": (
                "Grandmother sets the pot on top of the stove. I stand back while the "
                "curry begins to steam."
            ),
            "sentenceBn": (
                "দাদি চুলার ওপরে হাঁড়ি বসান। তরকারি থেকে বাষ্প উঠতে শুরু করলে আমি দূরে "
                "দাঁড়াই।"
            ),
        },
        4: {
            "sentenceEn": (
                "Dad washes the pan after lunch. The baby takes a nap while the kitchen "
                "is quiet."
            ),
            "sentenceBn": (
                "দুপুরের খাবারের পর বাবা কড়াই ধুয়ে ফেলেন। রান্নাঘর শান্ত থাকার সময় "
                "শিশুটি একটু ঘুমায়।"
            ),
        },
        5: {
            "sentenceEn": (
                "Grandfather ate a banana before his morning walk. When he returned, "
                "we sat down to eat breakfast together and drank warm tea."
            ),
            "sentenceBn": (
                "সকালের হাঁটার আগে দাদা একটি কলা খেয়েছিলেন। তিনি ফিরে এলে আমরা একসঙ্গে "
                "সকালের খাবার খেতে বসলাম এবং গরম চা পান করলাম।"
            ),
        },
        6: {
            "sentenceEn": (
                "Nila places a bowl on a low kitchen shelf. She fills it with fruit for "
                "lunch."
            ),
            "sentenceBn": (
                "নীলা রান্নাঘরের নিচু তাকে একটি বাটি রাখে। দুপুরের খাবারের জন্য সে বাটিটি "
                "ফল দিয়ে ভরে।"
            ),
        },
        7: {
            "sentenceEn": (
                "We sat on a mat to eat dinner together. My sister ate the meat curry, "
                "and I drank tea after the meal."
            ),
            "sentenceBn": (
                "আমরা একসঙ্গে রাতের খাবার খেতে মাদুরে বসেছিলাম। আমার বোন মাংসের তরকারি "
                "খেয়েছিল, আর আমি খাবারের পর চা পান করেছিলাম।"
            ),
        },
        8: {
            "sentenceEn": (
                "Rafi sat beside Grandma while she cooked. At last, she tasted the soup "
                "and added a pinch of salt."
            ),
            "sentenceBn": (
                "দাদি রান্না করার সময় রাফি তাঁর পাশে বসেছিল। শেষে দাদি স্যুপ চেখে এক "
                "চিমটি লবণ দিলেন।"
            ),
        },
        9: {
            "sentenceEn": (
                "Mina takes warm bread to the veranda. While she eats, she begins to "
                "read a library book."
            ),
            "sentenceBn": (
                "মিনা বারান্দায় গরম রুটি নিয়ে যায়। খেতে খেতে সে পাঠাগারের একটি বই পড়া "
                "শুরু করে।"
            ),
        },
        10: {
            "sentenceEn": (
                "A hen walks beside the kitchen garden. Mina spreads honey on one "
                "slice of toast for breakfast."
            ),
            "sentenceBn": (
                "একটি মুরগি রান্নাঘরের বাগানের পাশে হাঁটে। সকালের খাবারের জন্য মিনা এক "
                "টুকরা টোস্টে মধু মাখায়।"
            ),
        },
    },
    "clothing": {
        1: {
            "sentenceEn": (
                "Rupa asks what she should wear in the sun. Her mother gives her a wide "
                "hat."
            ),
            "sentenceBn": (
                "রূপা জিজ্ঞেস করে রোদে তার কী পরা উচিত। মা তাকে একটি চওড়া টুপি দেন।"
            ),
        },
        2: {
            "sentenceEn": (
                "Before the picnic, Arif helps pack the bag. He places his cap on top "
                "so he can reach it first."
            ),
            "sentenceBn": (
                "পিকনিকের আগে আরিফ ব্যাগে জিনিস ভরতে সাহায্য করে। সে ক্যাপটি ব্যাগের "
                "একেবারে ওপরে রাখে, যাতে সবার আগে সেটি হাতে পায়।"
            ),
        },
        3: {
            "sentenceEn": (
                "Mina sees mud on one shoe after watering the flowers. She rinses it "
                "gently with the hose."
            ),
            "sentenceBn": (
                "ফুলে পানি দেওয়ার পর মিনা একটি জুতায় কাদা দেখে। সে পানির নল দিয়ে "
                "জুতাটি আলতো করে ধুয়ে নেয়।"
            ),
        },
        4: {
            "sentenceEn": (
                "Please let me help with the buckle. I fasten the belt so it feels "
                "comfortable."
            ),
            "sentenceBn": (
                "দয়া করে আমাকে বাকল লাগাতে সাহায্য করতে দাও। আমি বেল্টটি এমনভাবে বাঁধি, "
                "যাতে আরাম লাগে।"
            ),
        },
        5: {
            "sentenceEn": (
                "Rafi wears a coat for the school play. In his act, he helps a lost cat "
                "find a warm home."
            ),
            "sentenceBn": (
                "স্কুলের নাটকের জন্য রাফি কোট পরে। তার অভিনয়ে সে হারিয়ে যাওয়া একটি "
                "বিড়ালকে উষ্ণ ঘর খুঁজে পেতে সাহায্য করে।"
            ),
        },
        6: {
            "sentenceEn": (
                "The vet puts on a clean vest before examining the calf. Her set of "
                "tools rests safely in a case."
            ),
            "sentenceBn": (
                "বাছুরটি পরীক্ষা করার আগে পশু চিকিৎসক পরিষ্কার ভেস্ট পরেন। তাঁর যন্ত্রের "
                "সেটটি বাক্সে নিরাপদে রাখা আছে।"
            ),
        },
        7: {
            "sentenceEn": (
                "It is hot, so Grandma chooses a light cloth for the shirt. The shop "
                "has a lot of this cloth because it feels cool at noon."
            ),
            "sentenceBn": (
                "গরম পড়েছে, তাই দাদি শার্টের জন্য হালকা কাপড় বেছে নেন। দুপুরে আরাম লাগে "
                "বলে দোকানে এই কাপড় অনেক আছে।"
            ),
        },
        8: {
            "sentenceEn": (
                "After playing in the sun, Rafi notices a light tan above his short "
                "pants. His mother gives him a gentle pat and reminds him to use "
                "sunscreen."
            ),
            "sentenceBn": (
                "রোদে খেলার পর রাফি তার ছোট প্যান্টের ওপরে ত্বকে হালকা বাদামি রং দেখে। "
                "মা তাকে আলতো চাপড় দিয়ে সানস্ক্রিন লাগানোর কথা মনে করিয়ে দেন।"
            ),
        },
        9: {
            "sentenceEn": (
                "Rafi and his brother sit beside a shop window. His brother points to a "
                "blue shirt and says this one is his, while a puppy waits beside its "
                "basket."
            ),
            "sentenceBn": (
                "রাফি ও তার ভাই দোকানের জানালার পাশে বসে। ভাইটি নীল শার্ট দেখিয়ে বলে "
                "এই শার্টটি তার, আর একটি কুকুরছানা নিজের ঝুড়ির পাশে অপেক্ষা করে।"
            ),
        },
        10: {
            "sentenceEn": (
                "Mina wraps a scarf around her neck before entering the car. Her family "
                "will travel far to visit Grandma."
            ),
            "sentenceBn": (
                "গাড়িতে ওঠার আগে মিনা গলায় স্কার্ফ জড়ায়। দাদিকে দেখতে তার পরিবার "
                "অনেক দূরে যাবে।"
            ),
        },
    },
    "numbers": {
        1: {
            "sentenceEn": (
                "Maya solves one puzzle at the end of class. When it is done, she checks "
                "her answer."
            ),
            "sentenceBn": (
                "ক্লাসের শেষে মায়া একটি ধাঁধা সমাধান করে। কাজটি হয়ে গেলে সে নিজের উত্তর "
                "যাচাই করে।"
            ),
        },
        2: {
            "sentenceEn": (
                "Two friends walk through the town market with their parents. They do "
                "not cross until the traffic officer signals."
            ),
            "sentenceBn": (
                "দুই বন্ধু মা-বাবার সঙ্গে শহরের বাজার দিয়ে হাঁটে। ট্রাফিক পুলিশ সংকেত না "
                "দেওয়া পর্যন্ত তারা রাস্তা পার হয় না।"
            ),
        },
        3: {
            "sentenceEn": (
                "A fisher counts ten small floats along his net. He checks each one "
                "before taking the boat out."
            ),
            "sentenceBn": (
                "এক জেলে তাঁর জালে লাগানো দশটি ছোট ভাসা গোনেন। নৌকা নিয়ে বের হওয়ার "
                "আগে তিনি প্রতিটি পরীক্ষা করেন।"
            ),
        },
        4: {
            "sentenceEn": (
                "We make four soft puppets for our class play. Each animal has fur made "
                "from clean yarn."
            ),
            "sentenceBn": (
                "আমরা আমাদের শ্রেণির নাটকের জন্য চারটি নরম পুতুল বানাই। প্রতিটি প্রাণীর "
                "পশম পরিষ্কার সুতা দিয়ে তৈরি।"
            ),
        },
        5: {
            "sentenceEn": (
                "A man carries many vegetables, including a large yam, through the "
                "market. A myna may copy his cheerful call from a nearby tree."
            ),
            "sentenceBn": (
                "এক ব্যক্তি একটি বড় মিষ্টি আলুসহ অনেক সবজি বাজারের ভেতর দিয়ে নিয়ে যান। "
                "কাছের গাছ থেকে একটি শালিক তাঁর আনন্দের ডাক নকল করতে পারে।"
            ),
        },
        6: {
            "sentenceEn": (
                "A pair of paper kites rises into the air. Hold both strings gently so "
                "they do not rip the paper."
            ),
            "sentenceBn": (
                "এক জোড়া কাগজের ঘুড়ি বাতাসে উড়ে ওঠে। দুটি সুতা আলতো করে ধরো, যাতে "
                "টানে কাগজ ছিঁড়ে না যায়।"
            ),
        },
        7: {
            "sentenceEn": (
                "A museum tray holds a piece of ore from a mine. We count the flowers "
                "growing on the bare rock and write zero."
            ),
            "sentenceBn": (
                "জাদুঘরের একটি ট্রেতে খনি থেকে আনা ধাতব আকরিকের টুকরা আছে। খালি পাথরে "
                "কতটি ফুল জন্মেছে তা গুনে আমরা শূন্য লিখি।"
            ),
        },
        8: {
            "sentenceEn": (
                "One page shows a fox asleep in its den. When Mina reaches the end of "
                "the dozen-page book, her counting is done."
            ),
            "sentenceBn": (
                "একটি পাতায় গর্তের ভেতর ঘুমন্ত শিয়ালের ছবি আছে। মিনা যখন বারো পাতার "
                "বইটির শেষে পৌঁছায়, তখন তার গোনা হয়ে যায়।"
            ),
        },
        9: {
            "sentenceEn": (
                "The children get eight soft balls for a number game. They try to hit "
                "the large target painted on the wall."
            ),
            "sentenceBn": (
                "শিশুরা সংখ্যার খেলার জন্য আটটি নরম বল পায়। তারা দেয়ালে আঁকা বড় "
                "লক্ষ্যে বল দিয়ে আঘাত করার চেষ্টা করে।"
            ),
        },
        10: {
            "sentenceEn": (
                "Mina places three number cards under the tree. Her sister points over "
                "there and finds the missing card."
            ),
            "sentenceBn": (
                "মিনা গাছের নিচে তিনটি সংখ্যার কার্ড রাখে। তার বোন সেখানে আঙুল দেখিয়ে "
                "হারানো কার্ডটি খুঁজে পায়।"
            ),
        },
    },
}


_EXPECTED_CATEGORIES = {
    "school",
    "transport",
    "weather",
    "food_kitchen",
    "clothing",
    "numbers",
}
_UPPERCASE_WORD_RE = re.compile(r"(?<![A-Za-z])[A-Z]{2,}(?![A-Za-z])")
_BENGALI_RE = re.compile(r"[\u0980-\u09ff]")


def _contains_whole_word(text: str, word: str) -> bool:
    return re.search(rf"(?<![A-Za-z]){re.escape(word)}(?![A-Za-z])", text, re.I) is not None


def _assert_prose(label: str, english: str, bengali: str) -> None:
    assert english.strip() == english and english, f"{label}: empty or padded English copy"
    assert bengali.strip() == bengali and bengali, f"{label}: empty or padded Bengali copy"
    assert _UPPERCASE_WORD_RE.search(english) is None, (
        f"{label}: all-uppercase token looks like word stuffing"
    )
    assert any(character.islower() for character in english), (
        f"{label}: English copy is not normal-case prose"
    )
    assert _BENGALI_RE.search(bengali), f"{label}: Bengali copy has no Bengali letters"


def validate(asset_path: Path | None = None) -> None:
    """Assert that the exported copy exactly covers its current asset targets."""

    if asset_path is None:
        asset_path = Path(__file__).resolve().parents[1] / "app/src/main/assets/categories.json"

    catalog = json.loads(asset_path.read_text(encoding="utf-8"))
    categories = {
        category["id"]: category
        for category in catalog["categories"]
        if category["id"] in _EXPECTED_CATEGORIES
    }

    assert set(categories) == _EXPECTED_CATEGORIES, "asset category keys changed"
    assert set(CATEGORY_STORIES) == set(categories), "category story keys do not match asset"
    assert set(LEVEL_STORIES) == set(categories), "level story category keys do not match asset"

    for category_id, category in categories.items():
        category_copy = CATEGORY_STORIES[category_id]
        assert set(category_copy) == {"storyEn", "storyBn"}, (
            f"{category_id}: unexpected category story fields"
        )
        _assert_prose(
            f"{category_id} story",
            category_copy["storyEn"],
            category_copy["storyBn"],
        )
        english_paragraphs = category_copy["storyEn"].split("\n\n")
        bengali_paragraphs = category_copy["storyBn"].split("\n\n")
        assert len(english_paragraphs) >= 2, (
            f"{category_id}: category story needs at least two paragraphs"
        )
        assert len(english_paragraphs) == len(bengali_paragraphs), (
            f"{category_id}: English and Bengali paragraph counts differ"
        )
        assert all(paragraph.strip() == paragraph and paragraph for paragraph in english_paragraphs), (
            f"{category_id}: malformed English paragraph break"
        )
        assert all(paragraph.strip() == paragraph and paragraph for paragraph in bengali_paragraphs), (
            f"{category_id}: malformed Bengali paragraph break"
        )
        missing_story_words = [
            word
            for word in category["storyWords"]
            if not _contains_whole_word(category_copy["storyEn"], word)
        ]
        assert not missing_story_words, (
            f"{category_id}: missing category story words {missing_story_words}"
        )

        asset_levels = {level["id"]: level for level in category["levels"]}
        assert set(LEVEL_STORIES[category_id]) == set(asset_levels), (
            f"{category_id}: level keys do not match asset"
        )

        for level_id, level in asset_levels.items():
            level_copy = LEVEL_STORIES[category_id][level_id]
            assert set(level_copy) == {"sentenceEn", "sentenceBn"}, (
                f"{category_id}/{level_id}: unexpected level story fields"
            )
            english = level_copy["sentenceEn"]
            bengali = level_copy["sentenceBn"]
            _assert_prose(f"{category_id}/{level_id}", english, bengali)
            sentence_count = len(re.findall(r'''[.!?](?=["”']?(?:\s|$))''', english))
            assert 2 <= sentence_count <= 4, (
                f"{category_id}/{level_id}: expected 2-4 English sentences, "
                f"found {sentence_count}"
            )
            missing_level_words = [
                word for word in level["words"] if not _contains_whole_word(english, word)
            ]
            assert not missing_level_words, (
                f"{category_id}/{level_id}: missing level words {missing_level_words}"
            )


if __name__ == "__main__":
    validate()
    print("Editorial daily copy validation passed.")
