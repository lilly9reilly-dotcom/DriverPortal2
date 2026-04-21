import SwiftUI

enum AppTheme {
    static let purple = Color(red: 108/255, green: 99/255, blue: 255/255)
    static let purpleDark = Color(red: 75/255, green: 66/255, blue: 217/255)
    static let orange = Color(red: 210/255, green: 122/255, blue: 43/255)
    static let softBackgroundTop = Color(red: 247/255, green: 243/255, blue: 255/255)
    static let softBackgroundBottom = Color(red: 241/255, green: 246/255, blue: 255/255)
    static let textDark = Color(red: 31/255, green: 36/255, blue: 48/255)
    static let textMuted = Color(red: 110/255, green: 117/255, blue: 130/255)
}

struct AppBackground: View {
    var body: some View {
        LinearGradient(
            colors: [AppTheme.softBackgroundTop, AppTheme.softBackgroundBottom],
            startPoint: .top,
            endPoint: .bottom
        )
        .ignoresSafeArea()
    }
}

struct HeroCard: View {
    let title: String
    let subtitle: String
    var icon: String = "sparkles"

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 8) {
                Text(title)
                    .font(.title2.bold())
                    .foregroundStyle(.white)
                Text(subtitle)
                    .foregroundStyle(.white.opacity(0.9))
                    .font(.subheadline)
            }
            Spacer()
            Image(systemName: icon)
                .font(.title2.weight(.bold))
                .foregroundStyle(.white)
                .padding(10)
                .background(.white.opacity(0.16))
                .clipShape(Circle())
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [AppTheme.purple, AppTheme.purpleDark],
                startPoint: .leading,
                endPoint: .trailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .shadow(color: .black.opacity(0.08), radius: 10, y: 4)
    }
}

struct SurfaceCard<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        content
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(.white)
            .clipShape(RoundedRectangle(cornerRadius: 20))
            .shadow(color: .black.opacity(0.05), radius: 8, y: 2)
    }
}

struct StatPill: View {
    let title: String
    let value: String
    let tint: Color

    var body: some View {
        VStack(spacing: 8) {
            Text(title)
                .font(.subheadline)
                .foregroundStyle(AppTheme.textMuted)
            Text(value)
                .font(.title3.bold())
                .foregroundStyle(AppTheme.textDark)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .background(tint.opacity(0.10))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

struct StatusBadge: View {
    let text: String
    let color: Color

    var body: some View {
        Text(text)
            .font(.caption.bold())
            .foregroundStyle(color)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(color.opacity(0.12))
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
