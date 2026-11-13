package com.example.data.firebase

import com.example.data.model.Bill
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.Project
import com.example.data.model.ProjectPayment
import com.example.data.model.ProjectPhase
import com.example.data.model.ProjectVendor
import com.example.data.model.ServiceItem
import com.example.data.model.ServiceVisit
import com.example.data.model.UserProfile
import com.example.data.model.Vehicle
import com.example.util.IndianFormatters

object SampleDataSeeder {

    val sixMonthCategoryBaseline: Map<String, Double> = mapOf(
        "home" to 105600.0,
        "biz" to 94000.0,
        "veh" to 26500.0,
        "groc" to 14000.0,
        "util" to 11700.0,
        "subs" to 4200.0,
        "hlth" to 5800.0,
        "food" to 2700.0
    )

    val priorFiveMonthsSpend: List<Pair<String, Double>> = listOf(
        "Jun" to 48200.0,
        "Jul" to 51900.0,
        "Aug" to 44300.0,
        "Sep" to 58700.0,
        "Oct" to 61400.0
    )

    val bikeDefaultTemplate = listOf(
        ServiceItem("oil", "Engine oil", "drop", 3000, 6),
        ServiceItem("oilf", "Oil filter", "wrench", 6000, 12),
        ServiceItem("lube", "Chain lube & tension", "link", 500, 1),
        ServiceItem("chain", "Chain & sprockets", "link", 20000, 36),
        ServiceItem("air", "Air filter", "wrench", 9000, 12),
        ServiceItem("brake", "Brake pads", "gear", 15000, 24),
        ServiceItem("align", "Wheel alignment & balance", "gear", 10000, 12),
        ServiceItem("ins", "Insurance renewal", "shield", 0, 12)
    )

    val carDefaultTemplate = listOf(
        ServiceItem("oil", "Engine oil & filter", "drop", 10000, 12),
        ServiceItem("tyre", "Tyre rotation & alignment", "gear", 10000, 12),
        ServiceItem("ac", "AC service", "wrench", 0, 12),
        ServiceItem("batt", "Battery check", "bolt", 0, 24),
        ServiceItem("air", "Air filter", "wrench", 15000, 24),
        ServiceItem("ins", "Insurance renewal", "shield", 0, 12)
    )

    val bikeExtraSuggestions = listOf(
        ServiceItem("spark", "Spark plug", "wrench", 8000, 12),
        ServiceItem("fork", "Fork oil", "drop", 12000, 24),
        ServiceItem("cables", "Clutch & brake cables", "link", 12000, 24),
        ServiceItem("bfluid", "Brake fluid", "drop", 0, 24),
        ServiceItem("tyres", "Tyres (replace)", "gear", 25000, 48),
        ServiceItem("battery", "Battery", "bolt", 0, 36),
        ServiceItem("bearings", "Wheel bearings", "wrench", 30000, 48)
    )

    val carExtraSuggestions = listOf(
        ServiceItem("sparks", "Spark plugs", "wrench", 30000, 36),
        ServiceItem("coolant", "Coolant", "drop", 40000, 24),
        ServiceItem("bfluid", "Brake fluid", "drop", 0, 24),
        ServiceItem("wipers", "Wiper blades", "wrench", 0, 12),
        ServiceItem("timing", "Timing belt", "gear", 60000, 60),
        ServiceItem("wbal", "Wheel balancing", "gear", 10000, 12),
        ServiceItem("puc", "Pollution certificate", "file", 0, 6)
    )

    val projectTemplates: Map<String, List<Pair<String, Int>>> = mapOf(
        "House build" to listOf(
            "Foundation & plinth" to 13,
            "Columns & slab" to 31,
            "Brickwork & plaster" to 16,
            "Electrical & plumbing" to 12,
            "Flooring & tiles" to 13,
            "Interiors & carpentry" to 15
        ),
        "Business setup" to listOf(
            "Registration & legal" to 6,
            "Deposit & rent advance" to 25,
            "Furniture & fit-out" to 37,
            "Equipment & software" to 22,
            "Launch marketing" to 10
        ),
        "Renovation" to listOf(
            "Demolition & prep" to 10,
            "Civil work" to 30,
            "Electrical & plumbing" to 20,
            "Tiles & flooring" to 20,
            "Painting & finishing" to 20
        ),
        "Event" to listOf(
            "Venue" to 35,
            "Catering" to 30,
            "Clothes & jewellery" to 20,
            "Photography" to 8,
            "Other" to 7
        )
    )

    fun defaultProfile(uid: String, displayName: String?): UserProfile = UserProfile(
        id = uid,
        userId = uid,
        monthlyBudget = 65000.0,
        language = "en",
        seeded = true,
        displayName = displayName
    )

    fun sampleCategories(uid: String): List<Category> = listOf(
        Category("veh", uid, "Vehicle", "car", 1, listOf("Fuel", "Service", "Insurance", "Parking & tolls", "Accessories"), 0),
        Category("home", uid, "Home & construction", "house", 2, listOf("Materials", "Labour", "Fixtures", "Repairs"), 1),
        Category("groc", uid, "Groceries & household", "bag", 3, listOf("Groceries", "Cleaning", "Kitchen"), 2),
        Category("util", uid, "Utilities", "bolt", 4, listOf("EB bill", "Water", "Gas", "Internet", "Mobile"), 3),
        Category("subs", uid, "Subscriptions", "tv", 5, listOf("OTT", "Music", "Apps & cloud"), 4),
        Category("hlth", uid, "Health", "heart", 6, listOf("Pharmacy", "Doctor", "Tests"), 5),
        Category("biz", uid, "Business", "building", 7, listOf("Rent", "Equipment", "Marketing", "Legal & fees"), 6),
        Category("food", uid, "Food & dining", "fork", 8, listOf("Restaurants", "Delivery", "Cafes"), 7),
        Category("fin", uid, "Loans & cards", "rupee", 9, listOf("Home loan EMI", "Credit card", "Personal loan"), 8)
    )

    fun sampleExpenses(uid: String, nowMs: Long): List<Expense> {
        fun d(daysAgo: Int): Long = IndianFormatters.addDays(nowMs, -daysAgo)
        return listOf(
            Expense("exp_1", uid, "Reliance Fresh", 2846.0, d(1), "groc", "Groceries", "UPI"),
            Expense("exp_2", uid, "Petrol, Classic 350", 1200.0, d(2), "veh", "Fuel", "UPI"),
            Expense("exp_3", uid, "Netflix", 649.0, d(3), "subs", "OTT", "Card"),
            Expense("exp_4", uid, "TNEB bill", 1184.0, d(5), "util", "EB bill", "UPI"),
            Expense("exp_5", uid, "Swiggy", 482.0, d(6), "food", "Delivery", "UPI"),
            Expense("exp_6", uid, "Pharmacy", 640.0, d(7), "hlth", "Pharmacy", "Cash"),
            Expense("exp_7", uid, "Cement, 50 bags", 19500.0, d(8), "home", "Materials", "Bank", projectId = "p1"),
            Expense("exp_8", uid, "Airtel broadband", 1179.0, d(9), "util", "Internet", "Card"),
            Expense("exp_9", uid, "Petrol, Classic 350", 1150.0, d(10), "veh", "Fuel", "UPI"),
            Expense("exp_10", uid, "Office rent", 18000.0, d(10), "biz", "Rent", "Bank", gstin = "33AABCU9603R1ZM", cgst = 1620.0, sgst = 1620.0, taxableValue = 14760.0),
            Expense("exp_11", uid, "YouTube Premium family", 189.0, d(11), "subs", "OTT", "Card")
        )
    }

    fun sampleVehicles(uid: String, nowMs: Long): List<Vehicle> {
        fun agoDays(days: Int): Long = IndianFormatters.addDays(nowMs, -days)
        fun agoMonths(m: Int, extraDays: Int = 0): Long =
            IndianFormatters.addDays(IndianFormatters.addMonths(nowMs, -m), -extraDays)

        val bikeItems = listOf(
            ServiceItem("oil", "Engine oil", "drop", 3000, 6, agoMonths(5, 9), 15100),
            ServiceItem("oilf", "Oil filter", "wrench", 6000, 12, agoMonths(5, 9), 15100),
            ServiceItem("chain", "Chain & sprockets", "link", 20000, 36, agoMonths(22), 2400),
            ServiceItem("lube", "Chain lube & tension", "link", 500, 1, agoDays(22), 18000),
            ServiceItem("align", "Wheel alignment & balance", "gear", 10000, 12, agoMonths(11, 22), 9800),
            ServiceItem("gen", "General service", "wrench", 6000, 6, agoMonths(5, 9), 15100),
            ServiceItem("air", "Air filter", "wrench", 9000, 12, agoMonths(5, 9), 15100),
            ServiceItem("brake", "Brake pads", "gear", 15000, 24, agoMonths(9), 12200),
            ServiceItem("ins", "Insurance renewal", "shield", 0, 12, agoMonths(11, 12), 0)
        )
        val bikeHistory = listOf(
            ServiceVisit(agoMonths(5, 9), 15100, "Sri Vel Two-Wheeler Care", 3840.0, listOf("Engine oil", "Oil filter", "Air filter", "General service"), "hist_1"),
            ServiceVisit(agoMonths(9), 12200, "Sri Vel Two-Wheeler Care", 2900.0, listOf("Brake pads", "Chain lube"), "hist_2"),
            ServiceVisit(agoMonths(11, 22), 9800, "Sri Vel Two-Wheeler Care", 1650.0, listOf("Wheel alignment", "Balance"), "hist_3")
        )

        val carItems = listOf(
            ServiceItem("oil", "Engine oil & filter", "drop", 10000, 12, agoMonths(7, 24), 34800),
            ServiceItem("tyre", "Tyre rotation & alignment", "gear", 10000, 12, agoMonths(11, 3), 30500),
            ServiceItem("ac", "AC service", "wrench", 0, 12, agoMonths(6, 20), 0),
            ServiceItem("batt", "Battery check", "bolt", 0, 24, agoMonths(21), 0),
            ServiceItem("ins", "Insurance renewal", "shield", 0, 12, agoMonths(10, 22), 0)
        )
        val carHistory = listOf(
            ServiceVisit(agoMonths(7, 24), 34800, "Kovai Maruti Service", 5200.0, listOf("Engine oil & filter", "Air filter"), "hist_c1")
        )

        return listOf(
            Vehicle(
                id = "v1",
                userId = uid,
                name = "Royal Enfield Classic 350",
                shortName = "Classic 350",
                type = "bike",
                plate = "TN 99 AB 4821",
                odometer = 18420,
                avgMileage = 34.0,
                items = bikeItems,
                history = bikeHistory
            ),
            Vehicle(
                id = "v2",
                userId = uid,
                name = "Maruti Swift VXi",
                shortName = "Swift",
                type = "car",
                plate = "TN 99 CD 1177",
                odometer = 41200,
                avgMileage = 17.0,
                items = carItems,
                history = carHistory
            )
        )
    }

    fun sampleBills(uid: String, nowMs: Long): List<Bill> {
        fun dOffset(days: Int): Long = IndianFormatters.addDays(nowMs, days)
        return listOf(
            Bill("b1", uid, "Property tax", 2150.0, dOffset(-2), "Half-yearly", "3 days before", "home", "Repairs", "house", autopay = false, paid = false),
            Bill("b2", uid, "TNEB electricity", 1420.0, dOffset(3), "Bi-monthly", "3 days before", "util", "EB bill", "bolt", autopay = false, paid = false),
            Bill("b3", uid, "HDFC credit card", 18260.0, dOffset(6), "Monthly", "3 days before", "fin", "Credit card", "rupee", autopay = false, paid = false),
            Bill("b4", uid, "Classic 350 insurance", 4120.0, dOffset(18), "Yearly", "7 days before", "veh", "Insurance", "shield", autopay = false, paid = false),
            Bill("b5", uid, "Netflix", 649.0, dOffset(16), "Monthly", "On the day", "subs", "OTT", "tv", autopay = true, paid = false),
            Bill("b6", uid, "Office rent", 18000.0, dOffset(19), "Monthly", "3 days before", "biz", "Rent", "building", autopay = false, paid = false),
            Bill("b8", uid, "Airtel broadband", 1179.0, dOffset(21), "Monthly", "3 days before", "util", "Internet", "wifi", autopay = false, paid = false),
            Bill("b7", uid, "Home loan EMI", 24500.0, dOffset(23), "Monthly", "3 days before", "fin", "Home loan EMI", "rupee", autopay = false, paid = false),
            Bill("b9", uid, "Home loan EMI", 24500.0, dOffset(-7), "Monthly", "3 days before", "fin", "Home loan EMI", "rupee", autopay = false, paid = true, paidOnEpochMs = dOffset(-7)),
            Bill("b10", uid, "Airtel broadband", 1179.0, dOffset(-9), "Monthly", "3 days before", "util", "Internet", "wifi", autopay = false, paid = true, paidOnEpochMs = dOffset(-9))
        )
    }

    fun sampleProjects(uid: String, nowMs: Long): List<Project> {
        fun ago(days: Int): Long = IndianFormatters.addDays(nowMs, -days)
        return listOf(
            Project(
                id = "p1",
                userId = uid,
                name = "House construction",
                subtitle = "Phase 2 · structure and finishing",
                iconKey = "house",
                colorIndex = 2,
                budget = 4500000.0,
                phases = listOf(
                    ProjectPhase("Foundation & plinth", 600000.0, 585000.0),
                    ProjectPhase("Columns & slab", 1400000.0, 1260000.0),
                    ProjectPhase("Brickwork & plaster", 700000.0, 420000.0),
                    ProjectPhase("Electrical & plumbing", 550000.0, 110000.0),
                    ProjectPhase("Flooring & tiles", 600000.0, 0.0),
                    ProjectPhase("Interiors & carpentry", 650000.0, 0.0)
                ),
                vendors = listOf(
                    ProjectVendor("Ravi Constructions", "Main contractor", 2800000.0, 2150000.0),
                    ProjectVendor("Sri Balaji Hardware", "Cement & steel", 900000.0, 812000.0),
                    ProjectVendor("Kumar Electricals", "Wiring and fittings", 350000.0, 110000.0)
                ),
                payments = listOf(
                    ProjectPayment(ago(8), "Cement, 50 bags", 19500.0, "Sri Balaji Hardware", "Brickwork & plaster"),
                    ProjectPayment(ago(15), "Slab shuttering labour", 180000.0, "Ravi Constructions", "Columns & slab"),
                    ProjectPayment(ago(24), "Steel, 2.4 tonnes", 168000.0, "Sri Balaji Hardware", "Columns & slab"),
                    ProjectPayment(ago(37), "Contractor, stage 4", 350000.0, "Ravi Constructions", "Columns & slab")
                )
            ),
            Project(
                id = "p2",
                userId = uid,
                name = "Studio setup",
                subtitle = "New company · registration and fit-out",
                iconKey = "building",
                colorIndex = 7,
                budget = 800000.0,
                phases = listOf(
                    ProjectPhase("Registration & legal", 45000.0, 38500.0),
                    ProjectPhase("Deposit & rent advance", 200000.0, 180000.0),
                    ProjectPhase("Furniture & fit-out", 300000.0, 120000.0),
                    ProjectPhase("Equipment & software", 180000.0, 46500.0),
                    ProjectPhase("Launch marketing", 75000.0, 0.0)
                ),
                vendors = listOf(
                    ProjectVendor("Aadhya Legal", "Company registration", 45000.0, 38500.0),
                    ProjectVendor("WorkNest Interiors", "Fit-out", 300000.0, 120000.0)
                ),
                payments = listOf(
                    ProjectPayment(ago(13), "Fit-out advance", 120000.0, "WorkNest Interiors", "Furniture & fit-out"),
                    ProjectPayment(ago(31), "Registration fees", 38500.0, "Aadhya Legal", "Registration & legal")
                )
            )
        )
    }
}
