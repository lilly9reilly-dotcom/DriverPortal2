import Foundation

struct ApiResponse: Codable {
    let success: Bool
    let message: String?
    let driver: String?
    let carNumber: String?
    let newDriver: Bool?
}

struct DriverLocation: Codable, Identifiable, Hashable {
    var id: String { "\(driver)|\(carNumber)" }
    let driver: String
    let carNumber: String
    let lat: Double
    let lng: Double
    let status: String
}

struct TripItem: Identifiable, Hashable {
    var id: String { docNumber + carNumber }
    let docNumber: String
    let carNumber: String
    let loadDate: String
    let unloadDate: String
    let quantity: String
    let station: String
    let price: String
    let date: String
    let status: String
}

struct WalletSummary {
    let trips: String
    let quantity: String
    let gas: String
    let salary: String
}

struct SavedDriverContact: Codable, Identifiable, Hashable {
    var id: String { "\(name)|\(carNumber)|\(phone)" }
    let name: String
    let carNumber: String
    let phone: String
}

struct CommunicationEntry: Identifiable, Hashable {
    var id: String { "\(name)|\(carNumber)" }
    let name: String
    let carNumber: String
    let phone: String
    let status: String
    let isCurrentDriver: Bool
}

struct TripSubmissionDraft {
    var docNumber = ""
    var loadDate = Date()
    var unloadDate = Date()
    var quantity = ""
    var liters = ""
    var ownerType = ""
    var destination = "محطة حلفاية"
    var factoryName = ""
    var factoryVoucher = ""
    var price = ""
    var notes = ""
    var fileData = ""
}

struct TripSubmissionRequest: Encodable {
    let action: String
    let docNumber: String
    let driverName: String
    let carNumber: String
    let loadDate: String
    let unloadDate: String
    let quantity: String
    let liters: String
    let ownerType: String
    let destination: String
    let factory: String
    let bojer: String
    let notes: String
    let price: String
    let fileData: String
}
