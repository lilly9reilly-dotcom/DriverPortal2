import Foundation

@MainActor
final class AppViewModel: ObservableObject {
    @Published var session = SessionStore()
    @Published var isLoggedIn = false
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var wallet = WalletSummary(trips: "0", quantity: "0", gas: "0", salary: "0")
    @Published var history: [TripItem] = []
    @Published var drivers: [DriverLocation] = []

    init() {
        isLoggedIn = session.isLoggedIn
        if isLoggedIn {
            Task { await refreshAll() }
        }
    }

    func signIn(name: String, phone: String, carNumber: String) async {
        isLoading = true
        errorMessage = nil

        do {
            let response = try await APIClient.shared.login(name: name, phone: phone, carNumber: carNumber)
            if response.success {
                session.save(
                    driverName: response.driver ?? name,
                    phone: phone,
                    carNumber: response.carNumber ?? carNumber
                )
                isLoggedIn = session.isLoggedIn
                await refreshAll()
            } else {
                errorMessage = response.message ?? "فشل تسجيل الدخول"
            }
        } catch {
            errorMessage = "تعذر الاتصال بالشبكة"
        }

        isLoading = false
    }

    func refreshAll() async {
        guard session.isLoggedIn else { return }
        isLoading = true
        errorMessage = nil

        do {
            async let walletCall = APIClient.shared.fetchWallet(driverName: session.driverName)
            async let historyCall = APIClient.shared.fetchHistory(driverName: session.driverName)
            async let driversCall = APIClient.shared.fetchDrivers()

            wallet = try await walletCall
            history = try await historyCall
            drivers = try await driversCall
        } catch {
            errorMessage = "تعذر تحديث البيانات حالياً"
        }

        isLoading = false
    }

    func logout() {
        session.logout()
        isLoggedIn = false
        history = []
        drivers = []
        wallet = WalletSummary(trips: "0", quantity: "0", gas: "0", salary: "0")
    }
}