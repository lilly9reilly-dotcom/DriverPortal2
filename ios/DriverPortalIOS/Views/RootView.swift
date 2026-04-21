import SwiftUI

struct RootView: View {
    @EnvironmentObject private var viewModel: AppViewModel

    var body: some View {
        Group {
            if viewModel.isLoggedIn {
                MainTabView()
            } else {
                LoginView()
            }
        }
        .environment(\.layoutDirection, .rightToLeft)
        .preferredColorScheme(.light)
    }
}