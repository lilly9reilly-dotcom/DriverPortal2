import SwiftUI

struct ProfileView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @State private var name = ""
    @State private var phone = ""
    @State private var carNumber = ""
    @State private var saveMessage: String?

    var body: some View {
        NavigationStack {
            ZStack {
                AppBackground()

                ScrollView {
                    VStack(spacing: 16) {
                        HeroCard(
                            title: "الحساب الشخصي",
                            subtitle: "إدارة بيانات السائق والدعم داخل نسخة الآيفون",
                            icon: "person.crop.circle"
                        )

                        SurfaceCard {
                            VStack(spacing: 12) {
                                TextField("اسم السائق", text: $name)
                                    .textFieldStyle(.roundedBorder)
                                TextField("رقم الهاتف", text: $phone)
                                    .textFieldStyle(.roundedBorder)
                                    .keyboardType(.phonePad)
                                TextField("رقم السيارة", text: $carNumber)
                                    .textFieldStyle(.roundedBorder)

                                Button("حفظ التعديلات") {
                                    viewModel.session.save(driverName: name, phone: phone, carNumber: carNumber)
                                    saveMessage = "تم حفظ البيانات"
                                }
                                .buttonStyle(.borderedProminent)
                                .tint(AppTheme.orange)
                            }
                        }

                        SurfaceCard {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("الدعم")
                                    .font(.headline.bold())
                                Text("رقم الدعم: \(GoogleSheetConfig.supportPhone)")
                                Text("واتساب: \(GoogleSheetConfig.supportWhatsApp)")
                            }
                        }

                        Button("تسجيل الخروج") {
                            viewModel.logout()
                        }
                        .buttonStyle(.bordered)
                        .tint(.red)

                        if let saveMessage {
                            SurfaceCard {
                                Text(saveMessage)
                                    .foregroundStyle(.green)
                            }
                        }
                    }
                    .padding(16)
                }
            }
            .navigationTitle("الحساب")
            .onAppear {
                name = viewModel.session.driverName
                phone = viewModel.session.phone
                carNumber = viewModel.session.carNumber
            }
        }
    }
}