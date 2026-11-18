package com.example.data.firebase

import com.example.data.model.Category
import com.example.data.model.ServiceItem
import com.example.data.model.UserProfile

object SampleDataSeeder {

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
        Category("fin", uid, "Loans & cards", "rupee", 9, listOf("Home loan EMI", "Credit card", "Personal loan"), 8),
        Category("inc", uid, "Income", "rupee", 4, listOf("Salary", "Business / Freelance", "Refund & cashback", "Interest & dividend", "Rental & other"), 9)
    )
}
