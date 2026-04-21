import Foundation

final class APIClient {
    static let shared = APIClient()
    private init() {}

    func login(name: String, phone: String, carNumber: String) async throws -> ApiResponse {
        var request = URLRequest(url: URL(string: GoogleSheetConfig.execEndpoint)!)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")

        let body = [
            "action=login",
            "name=\(encode(name))",
            "phone=\(encode(normalizePhone(phone)))",
            "carNumber=\(encode(carNumber))"
        ].joined(separator: "&")

        request.httpBody = body.data(using: .utf8)

        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(ApiResponse.self, from: data)
    }

    func fetchWallet(driverName: String) async throws -> WalletSummary {
        let url = GoogleSheetConfig.execURL(action: "wallet", params: ["driverName": driverName])
        let (data, _) = try await URLSession.shared.data(from: url)
        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]

        return WalletSummary(
            trips: readNumber(json["trips"]),
            quantity: readNumber(json["quantity"]),
            gas: readNumber(json["liters"]),
            salary: readNumber(json["profit"])
        )
    }

    func fetchHistory(driverName: String) async throws -> [TripItem] {
        let url = GoogleSheetConfig.execURL(action: "history", params: ["driverName": driverName])
        let (data, _) = try await URLSession.shared.data(from: url)
        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
        let trips = json["trips"] as? [[String: Any]] ?? []

        return trips.compactMap { trip in
            let docNumber = readText(trip["docNumber"])
            guard !docNumber.isEmpty else { return nil }

            return TripItem(
                docNumber: docNumber,
                carNumber: readText(trip["carNumber"]),
                loadDate: cleanDate(readText(trip["loadDate"])),
                unloadDate: cleanDate(readText(trip["unloadDate"])),
                quantity: cleanNumber(readText(trip["quantity"])),
                station: cleanStation(readText(trip["station"]), fallback: readText(trip["unloadDate"])),
                price: cleanNumber(readText(trip["price"])),
                date: cleanDate(readText(trip["date"])),
                status: readText(trip["status"]).isEmpty ? "ok" : readText(trip["status"])
            )
        }
    }

    func fetchDrivers() async throws -> [DriverLocation] {
        struct DriverListResponse: Codable {
            let success: Bool
            let drivers: [DriverLocation]
        }

        let url = GoogleSheetConfig.execURL(action: "drivers")
        let (data, _) = try await URLSession.shared.data(from: url)
        let response = try JSONDecoder().decode(DriverListResponse.self, from: data)
        return response.drivers.sorted {
            statusRank($0.status) == statusRank($1.status) ? $0.driver < $1.driver : statusRank($0.status) < statusRank($1.status)
        }
    }

    func checkDocNumber(_ docNumber: String) async throws -> Bool {
        var request = URLRequest(url: URL(string: GoogleSheetConfig.execEndpoint)!)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")

        let body = [
            "action=checkDoc",
            "docNumber=\(encode(docNumber))"
        ].joined(separator: "&")

        request.httpBody = body.data(using: .utf8)

        let (data, _) = try await URLSession.shared.data(for: request)
        let response = try JSONDecoder().decode(ApiResponse.self, from: data)
        return response.message == "EXISTS"
    }

    func submitTrip(draft: TripSubmissionDraft, driverName: String, carNumber: String) async throws -> ApiResponse {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]

        let payload = TripSubmissionRequest(
            action: "trip",
            docNumber: draft.docNumber,
            driverName: driverName,
            carNumber: carNumber,
            loadDate: formatter.string(from: draft.loadDate),
            unloadDate: formatter.string(from: draft.unloadDate),
            quantity: draft.quantity,
            liters: draft.liters,
            ownerType: draft.ownerType,
            destination: draft.destination,
            factory: "",
            bojer: "",
            notes: mergeTripNotes(
                baseNotes: draft.notes,
                factoryName: draft.factoryName,
                factoryVoucher: draft.factoryVoucher
            ),
            price: draft.price,
            fileData: draft.fileData
        )

        var request = URLRequest(url: URL(string: GoogleSheetConfig.execEndpoint)!)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(payload)

        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(ApiResponse.self, from: data)
    }

    private func mergeTripNotes(baseNotes: String, factoryName: String, factoryVoucher: String) -> String {
        let trimmedNotes = baseNotes.trimmingCharacters(in: .whitespacesAndNewlines)
        var extras: [String] = []

        let trimmedFactory = factoryName.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmedFactory.isEmpty {
            extras.append("اسم المعمل: \(trimmedFactory)")
        }

        let trimmedVoucher = factoryVoucher.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmedVoucher.isEmpty {
            extras.append("رقم بوچر المعمل: \(trimmedVoucher)")
        }

        guard !extras.isEmpty else { return trimmedNotes }
        guard !trimmedNotes.isEmpty else { return extras.joined(separator: " | ") }
        return "\(trimmedNotes) - \(extras.joined(separator: " | "))"
    }

    private func readNumber(_ value: Any?) -> String {
        if let number = value as? NSNumber {
            return "\(number)"
        }
        return readText(value)
    }

    private func readText(_ value: Any?) -> String {
        if let text = value as? String {
            return text.trimmingCharacters(in: .whitespacesAndNewlines)
        }
        if let number = value as? NSNumber {
            return number.stringValue
        }
        return ""
    }

    private func cleanNumber(_ value: String) -> String {
        guard !value.lowercased().contains("http") else { return "" }
        return value
    }

    private func cleanDate(_ value: String) -> String {
        guard value.contains("GMT") || value.contains("202") || value.contains("/") else { return value }
        return value
    }

    private func cleanStation(_ value: String, fallback: String) -> String {
        if !value.isEmpty { return value }
        if !fallback.contains("GMT") && !fallback.isEmpty { return fallback }
        return "-"
    }

    private func encode(_ value: String) -> String {
        value.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? value
    }

    private func normalizePhone(_ phone: String) -> String {
        let clean = phone.filter { $0.isNumber }
        if clean.hasPrefix("0") { return String(clean.dropFirst()) }
        return clean
    }

    private func statusRank(_ status: String) -> Int {
        switch status.lowercased() {
        case "online": return 0
        case "stopped": return 1
        default: return 2
        }
    }
}
