package com.callguard.app

/**
 * In-app knowledge base. The source-of-truth guidance that used to live only in
 * the project's .md files now ships inside the app so users can read it offline.
 * USA-focused: the traceback path described here is federal (FCC / TRACED Act),
 * so it holds in every state — only the local police and state AG contacts differ.
 */
data class Article(
    val title: String,
    val summary: String,
    val body: List<Section>,
)

data class Section(val heading: String, val paragraphs: List<String>)

object LearnContent {

    val articles: List<Article> = listOf(
        Article(
            title = "What CallGuard can and can't do",
            summary = "Why an app can't unmask a spoofed caller — and what it does instead.",
            body = listOf(
                Section(
                    "The honest limit",
                    listOf(
                        "Your phone only ever sees the number the network hands it. When a caller " +
                            "spoofs their caller ID, that number is fake — so no app on your phone, " +
                            "CallGuard included, can reveal who is really calling.",
                        "Only your carrier and law enforcement can trace the true origin, through a " +
                            "process called a traceback (often backed by a subpoena).",
                    )
                ),
                Section(
                    "What the app is for",
                    listOf(
                        "CallGuard turns your call log into organized, timestamped, court-ready " +
                            "evidence. It flags the \"silent stranger\" pattern, counts how many " +
                            "different numbers are hitting you, and builds the exact documents the " +
                            "FCC, your carrier, and the police need to start a traceback.",
                        "Think of it as the evidence binder that makes officials act — not the " +
                            "magnifying glass that names the caller.",
                    )
                ),
            )
        ),
        Article(
            title = "How a traceback actually works",
            summary = "The federal path that unmasks a spoofed caller, step by step.",
            body = listOf(
                Section(
                    "The chain",
                    listOf(
                        "1. You document the calls (CallGuard does this).",
                        "2. You file an FCC complaint — this feeds the federal record of spoofing campaigns.",
                        "3. You file a police report and open a carrier harassment case.",
                        "4. Police subpoena your carrier. The carrier works backward through each " +
                            "network the call passed through — the \"traceback\" — until they reach " +
                            "the originating provider and the real line.",
                    )
                ),
                Section(
                    "Why your evidence matters",
                    listOf(
                        "Tracebacks are driven by the Industry Traceback Group and enabled by the " +
                            "federal TRACED Act. Officials prioritize cases with clean documentation: " +
                            "dates, counts, the number of distinct spoofed numbers, and a described " +
                            "pattern. That is exactly what CallGuard's CSV and PDF provide.",
                    )
                ),
            )
        ),
        Article(
            title = "Right after a harassing call",
            summary = "The one thing to do in the moment, plus what not to bother with.",
            body = listOf(
                Section(
                    "Call Trace (*57)",
                    listOf(
                        "On a landline, dialing *57 immediately after a harassing call tells the " +
                            "carrier to log the true originating line for that specific call, in a " +
                            "form police can subpoena. You won't see the result — by design it goes " +
                            "to the carrier and law enforcement. It usually carries a small per-use fee.",
                        "Reality for mobile: *57 is a landline feature and is not reliable on wireless. " +
                            "On a cell phone, the path that unmasks a spoofed caller is the police " +
                            "report → carrier subpoena chain, not *57.",
                    )
                ),
                Section(
                    "In the moment",
                    listOf(
                        "Don't engage or call back. Note the time and what happened (silent? recording? " +
                            "hung up?) — in CallGuard, tap the call and add a note. Those notes ride " +
                            "along in your export and strengthen the pattern.",
                    )
                ),
            )
        ),
        Article(
            title = "Reduce the volume: carrier + phone tools",
            summary = "Free blocking tools that cut the flood while you build your case.",
            body = listOf(
                Section(
                    "Carrier spam blocking",
                    listOf(
                        "Most major US carriers offer a free spam-blocking app or setting (for " +
                            "example AT&T ActiveArmor, Verizon Call Filter, T-Mobile Scam Shield). " +
                            "Turn on spam-risk blocking and unknown-caller handling. These won't " +
                            "identify a spoofed number, but they cut the volume and log spam categories.",
                    )
                ),
                Section(
                    "Built-in phone settings",
                    listOf(
                        "Android's Phone app has \"Filter spam calls,\" and you can enable " +
                            "\"Silence unknown callers\" so numbers not in your contacts don't ring " +
                            "through. CallGuard keeps logging them in parallel so your evidence stays complete.",
                    )
                ),
            )
        ),
        Article(
            title = "Register and stay protected",
            summary = "Do-Not-Call registration and keeping your evidence clean.",
            body = listOf(
                Section(
                    "Do Not Call registry",
                    listOf(
                        "Register your number free at donotcall.gov. It won't stop illegal spoofers, " +
                            "but being registered strengthens your FCC complaint (you can state you " +
                            "gave no consent and are on the registry).",
                    )
                ),
                Section(
                    "Keep your evidence clean",
                    listOf(
                        "Export regularly so you have dated snapshots. Don't delete the harassing " +
                            "calls from your phone log — they are the evidence. Add notes while the " +
                            "details are fresh.",
                    )
                ),
            )
        ),
        Article(
            title = "Glossary",
            summary = "Spoofing, traceback, subpoena, and the terms officials will use.",
            body = listOf(
                Section(
                    "Terms",
                    listOf(
                        "Caller ID spoofing — faking the number that shows on your screen so the " +
                            "call looks local or trusted.",
                        "Traceback — the carrier-to-carrier process of following a call back through " +
                            "each network to its true origin.",
                        "Subpoena — a legal order (obtained by police) compelling your carrier to " +
                            "hand over the originating records.",
                        "TRACED Act — the 2019 federal law that strengthened penalties for illegal " +
                            "spoofing and mandated call-authentication (STIR/SHAKEN).",
                        "STIR/SHAKEN — the carrier framework that cryptographically signs caller ID " +
                            "to make spoofing harder to pass through.",
                    )
                ),
            )
        ),
    )
}
