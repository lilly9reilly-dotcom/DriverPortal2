import Foundation

enum GoogleSheetConfig {
    static let apiScriptRoot = "https://script.google.com/macros/s/AKfycbwQsUx8PVIIPufmI8Ev0tTy6qEBtcNn7LXldhmCnuPwpq0VfZUjAx8pl13jSWxywvRM9A/"
    static let adminScriptRoot = "https://script.google.com/macros/s/AKfycbwQsUx8PVIIPufmI8Ev0tTy6qEBtcNn7LXldhmCnuPwpq0VfZUjAx8pl13jSWxywvRM9A/"
    static let execEndpoint = apiScriptRoot + "exec"
    static let adminPageURL = adminScriptRoot + "exec?page=admin"
    static let supportPhone = "07809830249"
    static let supportWhatsApp = "9647809830249"

    static func execURL(action: String, params: [String: String] = [:]) -> URL {
        var components = URLComponents(string: execEndpoint)!
        var queryItems = [URLQueryItem(name: "action", value: action)]
        for (key, value) in params where !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            queryItems.append(URLQueryItem(name: key, value: value))
        }
        components.queryItems = queryItems
        return components.url!
    }
}