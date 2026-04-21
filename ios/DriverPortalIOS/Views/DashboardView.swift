import SwiftUI

struct DashboardView: View {
    @EnvironmentObject private var viewModel: AppViewModel

    var body: some View {
        NavigationStack {
            ZStack {
                AppBackground()

                ScrollView {
                    VStack(spacing: 16) {
                        HeroCard(
                            title: "مرحباً، \(viewModel.session.driverName)",
                            subtitle: "السيارة: \(viewModel.session.carNumber) • الهاتف: \(viewModel.session.phone)",
                            icon: "person.text.rectangle"
                        )

                        HStack(spacing: 12) {
                            StatPill(title: "النقلات", value: viewModel.wallet.trips, tint: AppTheme.purple)
                            StatPill(title: "الكمية", value: viewModel.wallet.quantity, tint: .teal)
                        }

                        HStack(spacing: 12) {
                            StatPill(title: "الكاز", value: viewModel.wallet.gas, tint: AppTheme.orange)
                            StatPill(title: "الربح", value: viewModel.wallet.salary, tint: .green)
                        }

                        SurfaceCard {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("إدارة الحساب")
                                    .font(.headline.bold())
                                    .foregroundStyle(AppTheme.textDark)

                                Button("تحديث البيانات") {
                                    Task { await viewModel.refreshAll() }
                                }
                                .buttonStyle(.borderedProminent)
                                .tint(AppTheme.orange)

                                Button("تسجيل الخروج") {
                                    viewModel.logout()
                                }
                                .buttonStyle(.bordered)
                                .tint(.red)
                            }
                        }
                    }
                    .padding(16)
                }
            }
            .navigationTitle("اللوحة")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("تحديث") {
                        Task { await viewModel.refreshAll() }
                    }
                }
            }
        }
    }
}