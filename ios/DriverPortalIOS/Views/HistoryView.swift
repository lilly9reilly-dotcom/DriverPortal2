import SwiftUI

struct HistoryView: View {
    @EnvironmentObject private var viewModel: AppViewModel

    var body: some View {
        NavigationStack {
            ZStack {
                AppBackground()

                ScrollView {
                    LazyVStack(spacing: 12) {
                        HeroCard(
                            title: "سجل الوصلات",
                            subtitle: "متابعة جميع الوصلات الخاصة بالسائق بشكل واضح ومرتب",
                            icon: "doc.text.magnifyingglass"
                        )

                        if viewModel.history.isEmpty {
                            SurfaceCard {
                                Text("لا يوجد سجل حالياً")
                                    .foregroundStyle(AppTheme.textMuted)
                            }
                        }

                        ForEach(viewModel.history) { trip in
                            SurfaceCard {
                                VStack(alignment: .leading, spacing: 10) {
                                    HStack {
                                        Text("وصل #\(trip.docNumber)")
                                            .font(.headline.bold())
                                            .foregroundStyle(AppTheme.textDark)
                                        Spacer()
                                        StatusBadge(
                                            text: trip.quantity.isEmpty ? "-" : "\(trip.quantity) طن",
                                            color: AppTheme.purple
                                        )
                                    }

                                    Text("السيارة: \(trip.carNumber.isEmpty ? "-" : trip.carNumber)")
                                    Text("المحطة: \(trip.station.isEmpty ? "-" : trip.station)")
                                    Text("سعر النقلة: \(trip.price.isEmpty ? "-" : trip.price)")
                                    Text("تاريخ التحميل: \(trip.loadDate.isEmpty ? "-" : trip.loadDate)")
                                    Text("تاريخ التفريغ: \(trip.unloadDate.isEmpty ? "-" : trip.unloadDate)")

                                    if !trip.date.isEmpty {
                                        Text("تاريخ الإدخال: \(trip.date)")
                                            .foregroundStyle(AppTheme.textMuted)
                                    }
                                }
                            }
                        }
                    }
                    .padding(16)
                }
            }
            .navigationTitle("السجل")
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