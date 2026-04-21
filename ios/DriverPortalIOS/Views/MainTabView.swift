import SwiftUI

struct MainTabView: View {
    var body: some View {
        TabView {
            TripFormView()
                .tabItem {
                    Label("وصل", systemImage: "doc.badge.plus")
                }

            DashboardView()
                .tabItem {
                    Label("اللوحة", systemImage: "square.grid.2x2")
                }

            HistoryView()
                .tabItem {
                    Label("السجل", systemImage: "doc.text")
                }

            CommunicationView()
                .tabItem {
                    Label("التواصل", systemImage: "phone.badge.waveform")
                }

            MoreView()
                .tabItem {
                    Label("المزيد", systemImage: "line.3.horizontal")
                }
        }
        .tint(.white)
    }
}