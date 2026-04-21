import Foundation

final class SessionStore: ObservableObject {
    @Published var driverName: String
    @Published var phone: String
    @Published var carNumber: String

    init() {
        let defaults = UserDefaults.standard
        self.driverName = defaults.string(forKey: "ios_driver_name") ?? ""
        self.phone = defaults.string(forKey: "ios_driver_phone") ?? ""
        self.carNumber = defaults.string(forKey: "ios_driver_car") ?? ""
    }

    var isLoggedIn: Bool {
        !driverName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    func save(driverName: String, phone: String, carNumber: String) {
        self.driverName = driverName
        self.phone = phone
        self.carNumber = carNumber

        let defaults = UserDefaults.standard
        defaults.set(driverName, forKey: "ios_driver_name")
        defaults.set(phone, forKey: "ios_driver_phone")
        defaults.set(carNumber, forKey: "ios_driver_car")
    }

    func logout() {
        driverName = ""
        phone = ""
        carNumber = ""

        let defaults = UserDefaults.standard
        defaults.removeObject(forKey: "ios_driver_name")
        defaults.removeObject(forKey: "ios_driver_phone")
        defaults.removeObject(forKey: "ios_driver_car")
    }
}