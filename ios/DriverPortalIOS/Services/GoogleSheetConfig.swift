import Foundation

enum GoogleSheetConfig {
    static let apiScriptRoot = "https://script.google.com/macros/s/AKfycbwCreVvebaAN7C4W2OZu6ura7cza42P2lIssNt4sVBv1raDqZkQYY-ZZyNNcl9_iynhAw/"
    static let adminScriptRoot = "https://script.google.com/macros/s/AKfycbwCreVvebaAN7C4W2OZu6ura7cza42P2lIssNt4sVBv1raDqZkQYY-ZZyNNcl9_iynhAw/"
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