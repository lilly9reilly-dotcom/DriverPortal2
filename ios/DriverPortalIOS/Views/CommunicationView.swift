import SwiftUI

struct CommunicationView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @Environment(\.openURL) private var openURL

    @AppStorage("ios_saved_contacts") private var contactsData = "[]"
    @State private var name = ""
    @State private var carNumber = ""
    @State private var phone = ""

    private var savedContacts: [SavedDriverContact] {
        guard let data = contactsData.data(using: .utf8),
              let contacts = try? JSONDecoder().decode([SavedDriverContact].self, from: data) else {
            return []
        }
        return contacts
    }

    private var directory: [CommunicationEntry] {
        var result: [CommunicationEntry] = []
        var inserted = Set<String>()

        for item in savedContacts {
            let key = item.name + item.carNumber
            guard !inserted.contains(key) else { continue }
            inserted.insert(key)
            result.append(
                CommunicationEntry(
                    name: item.name,
                    carNumber: item.carNumber,
                    phone: item.phone,
                    status: "offline",
                    isCurrentDriver: item.name == viewModel.session.driverName
                )
            )
        }

        for driver in viewModel.drivers {
            let key = driver.driver + driver.carNumber
            guard !inserted.contains(key) else { continue }
            inserted.insert(key)
            let matchedPhone = savedContacts.first(where: { $0.name == driver.driver || $0.carNumber == driver.carNumber })?.phone ?? ""
            result.append(
                CommunicationEntry(
                    name: driver.driver,
                    carNumber: driver.carNumber,
                    phone: matchedPhone,
                    status: driver.status,
                    isCurrentDriver: driver.driver == viewModel.session.driverName
                )
            )
        }

        return result.sorted { $0.name < $1.name }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                AppBackground()

                ScrollView {
                    VStack(spacing: 14) {
                        HeroCard(
                            title: "مركز التواصل",
                            subtitle: "الاتصال والمراسلة والدعم داخل نسخة الآيفون",
                            icon: "phone.connection"
                        )

                        SurfaceCard {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("رقم الدعم الرسمي")
                                    .font(.headline.bold())
                                    .foregroundStyle(AppTheme.textDark)
                                Text(GoogleSheetConfig.supportPhone)
                                    .font(.title3.bold())
                                HStack {
                                    Button("اتصال الدعم") {
                                        openPhone(GoogleSheetConfig.supportPhone)
                                    }
                                    .buttonStyle(.borderedProminent)
                                    .tint(AppTheme.orange)

                                    Button("واتساب") {
                                        openWhatsApp(GoogleSheetConfig.supportWhatsApp)
                                    }
                                    .buttonStyle(.bordered)
                                }
                            }
                        }

                        SurfaceCard {
                            VStack(spacing: 10) {
                                TextField("اسم السائق", text: $name)
                                    .textFieldStyle(.roundedBorder)
                                TextField("رقم السيارة", text: $carNumber)
                                    .textFieldStyle(.roundedBorder)
                                TextField("رقم الهاتف", text: $phone)
                                    .textFieldStyle(.roundedBorder)
                                    .keyboardType(.phonePad)

                                Button("حفظ أو تحديث الرقم") {
                                    saveContact()
                                }
                                .buttonStyle(.borderedProminent)
                                .tint(AppTheme.purple)
                            }
                        }

                        ForEach(directory) { entry in
                            SurfaceCard {
                                VStack(alignment: .leading, spacing: 10) {
                                    HStack {
                                        VStack(alignment: .leading, spacing: 4) {
                                            Text(entry.name)
                                                .font(.headline.bold())
                                                .foregroundStyle(AppTheme.textDark)
                                            Text("السيارة: \(entry.carNumber.isEmpty ? "-" : entry.carNumber)")
                                                .foregroundStyle(AppTheme.textMuted)
                                        }
                                        Spacer()
                                        StatusBadge(text: statusLabel(entry.status), color: statusColor(entry.status))
                                    }

                                    Text("الهاتف: \(entry.phone.isEmpty ? "غير مضاف بعد" : entry.phone)")
                                        .fontWeight(entry.phone.isEmpty ? .regular : .semibold)
                                        .foregroundStyle(entry.phone.isEmpty ? AppTheme.textMuted : AppTheme.textDark)

                                    HStack {
                                        Button("اتصال") { openPhone(entry.phone) }
                                            .buttonStyle(.borderedProminent)
                                            .tint(.green)
                                            .disabled(entry.phone.isEmpty)
                                        Button("مراسلة") { openWhatsApp(normalizePhone(entry.phone)) }
                                            .buttonStyle(.bordered)
                                            .disabled(entry.phone.isEmpty)
                                        Button("تعديل") {
                                            name = entry.name
                                            carNumber = entry.carNumber
                                            phone = entry.phone
                                        }
                                        .buttonStyle(.bordered)
                                    }
                                }
                            }
                        }
                    }
                    .padding(16)
                }
            }
            .navigationTitle("التواصل")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("تحديث") {
                        Task { await viewModel.refreshAll() }
                    }
                }
            }
        }
    }

    private func saveContact() {
        guard !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              !phone.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }

        var contacts = savedContacts.filter { !($0.name == name || $0.carNumber == carNumber || $0.phone == phone) }
        contacts.insert(SavedDriverContact(name: name, carNumber: carNumber, phone: phone), at: 0)

        if let data = try? JSONEncoder().encode(contacts),
           let text = String(data: data, encoding: .utf8) {
            contactsData = text
        }
    }

    private func openPhone(_ phone: String) {
        guard let url = URL(string: "tel://\(phone.filter { $0.isNumber })") else { return }
        openURL(url)
    }

    private func openWhatsApp(_ phone: String) {
        guard let url = URL(string: "https://wa.me/\(phone.filter { $0.isNumber })") else { return }
        openURL(url)
    }

    private func normalizePhone(_ phone: String) -> String {
        let clean = phone.filter { $0.isNumber }
        if clean.hasPrefix("0") { return "964" + clean.dropFirst() }
        return clean
    }

    private func statusLabel(_ status: String) -> String {
        switch status.lowercased() {
        case "online": return "متصل"
        case "stopped": return "متوقف"
        default: return "غير متصل"
        }
    }

    private func statusColor(_ status: String) -> Color {
        switch status.lowercased() {
        case "online": return .green
        case "stopped": return .orange
        default: return .gray
        }
    }
}