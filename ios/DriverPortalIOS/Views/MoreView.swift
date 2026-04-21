import SwiftUI

struct MoreView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @Environment(\.openURL) private var openURL

    var body: some View {
        NavigationStack {
            ZStack {
                AppBackground()

                ScrollView {
                    VStack(spacing: 14) {
                        HeroCard(
                            title: "المزيد",
                            subtitle: "الوصول السريع إلى الأقسام الإضافية والدعم والحساب",
                            icon: "ellipsis.circle"
                        )

                        SurfaceCard {
                            VStack(spacing: 12) {
                                MoreActionRow(
                                    title: "الحساب",
                                    subtitle: "بيانات السائق الشخصية",
                                    systemImage: "person.crop.circle",
                                    tint: .blue
                                ) {
                                    ProfileView()
                                }

                                MoreActionRow(
                                    title: "السجل",
                                    subtitle: "استعراض الوصلات السابقة",
                                    systemImage: "doc.text",
                                    tint: AppTheme.purple
                                ) {
                                    HistoryView()
                                }

                                MoreActionRow(
                                    title: "التواصل",
                                    subtitle: "أرقام السواق والدعم",
                                    systemImage: "phone.badge.waveform",
                                    tint: .green
                                ) {
                                    CommunicationView()
                                }
                            }
                        }

                        SurfaceCard {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("الدعم السريع")
                                    .font(.headline.bold())
                                    .foregroundStyle(AppTheme.textDark)

                                Text("رقم الدعم: \(GoogleSheetConfig.supportPhone)")
                                    .foregroundStyle(AppTheme.textMuted)

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

                                Button("تحديث جميع البيانات") {
                                    Task { await viewModel.refreshAll() }
                                }
                                .buttonStyle(.bordered)
                            }
                        }
                    }
                    .padding(16)
                }
            }
            .navigationTitle("المزيد")
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
}

private struct MoreActionRow<Destination: View>: View {
    let title: String
    let subtitle: String
    let systemImage: String
    let tint: Color
    let destination: Destination

    init(title: String, subtitle: String, systemImage: String, tint: Color, @ViewBuilder destination: () -> Destination) {
        self.title = title
        self.subtitle = subtitle
        self.systemImage = systemImage
        self.tint = tint
        self.destination = destination()
    }

    var body: some View {
        NavigationLink(destination: destination) {
            HStack(spacing: 12) {
                Image(systemName: systemImage)
                    .font(.title3.bold())
                    .foregroundStyle(tint)
                    .frame(width: 44, height: 44)
                    .background(tint.opacity(0.12))
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.headline.bold())
                        .foregroundStyle(AppTheme.textDark)
                    Text(subtitle)
                        .font(.subheadline)
                        .foregroundStyle(AppTheme.textMuted)
                }

                Spacer()
                Image(systemName: "chevron.right")
                    .foregroundStyle(tint)
            }
            .padding(.vertical, 4)
        }
    }
}