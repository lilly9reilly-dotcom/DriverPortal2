import SwiftUI

struct LoginView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @State private var name = ""
    @State private var phone = ""
    @State private var carNumber = ""

    var body: some View {
        NavigationStack {
            ZStack {
                AppBackground()

                ScrollView {
                    VStack(spacing: 18) {
                        HeroCard(
                            title: "بوابة السائق",
                            subtitle: "نسخة iPhone الخاصة بالسواق مع نفس النظام الحالي",
                            icon: "iphone"
                        )
                        .padding(.top, 8)

                        SurfaceCard {
                            VStack(spacing: 14) {
                                TextField("اسم السائق", text: $name)
                                    .textFieldStyle(.roundedBorder)
                                TextField("رقم الهاتف", text: $phone)
                                    .textFieldStyle(.roundedBorder)
                                    .keyboardType(.phonePad)
                                TextField("رقم السيارة", text: $carNumber)
                                    .textFieldStyle(.roundedBorder)
                            }
                        }

                        Button {
                            Task {
                                await viewModel.signIn(name: name, phone: phone, carNumber: carNumber)
                            }
                        } label: {
                            HStack {
                                if viewModel.isLoading { ProgressView().tint(.white) }
                                Text("دخول السائق")
                                    .fontWeight(.bold)
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(AppTheme.orange)
                            .foregroundStyle(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                        }

                        if let error = viewModel.errorMessage {
                            SurfaceCard {
                                Text(error)
                                    .foregroundStyle(.red)
                            }
                        }
                    }
                    .padding(16)
                }
            }
        }
    }
}