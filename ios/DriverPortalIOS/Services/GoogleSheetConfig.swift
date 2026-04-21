import Foundation

enum GoogleSheetConfig {
    static let scriptRoot = "https://script.google.com/macros/s/AKfycbw-3wKRuKImCvvB4ip3PGokDP18yJz6HDW2QylDmvQGxAbyn8Wq-FIlHQ9ms-i7wlCEQA/"
    static let execEndpoint = scriptRoot + "exec"
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