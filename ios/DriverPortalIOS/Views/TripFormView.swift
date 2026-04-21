import SwiftUI
import PhotosUI

struct TripFormView: View {
    @EnvironmentObject private var viewModel: AppViewModel
    @State private var draft = TripSubmissionDraft()
    @State private var selectedPhoto: PhotosPickerItem?
    @State private var isSubmitting = false
    @State private var resultMessage: String?

    private let stations = ["محطة حلفاية", "محطة التاجي", "محطات الشمال", "أخرى"]
    private let factories = [
        "مستودع التاجي",
        "أبو غريب",
        "غزالية",
        "عامرية",
        "صمود",
        "زعفرانية",
        "مشتل",
        "نهضة",
        "رصافة",
        "نهروان",
        "حبيبية",
        "طارق",
        "بوب الشام",
        "كسرى وعطش",
        "أخرى"
    ]

    var body: some View {
        NavigationStack {
            ZStack {
                AppBackground()

                ScrollView {
                    VStack(spacing: 16) {
                        HeroCard(
                            title: "وصل السائق",
                            subtitle: "السائق: \(viewModel.session.driverName) • السيارة: \(viewModel.session.carNumber)",
                            icon: "doc.badge.plus"
                        )

                        SurfaceCard {
                            VStack(spacing: 12) {
                                TextField("رقم الوصل", text: $draft.docNumber)
                                    .textFieldStyle(.roundedBorder)
                                    .keyboardType(.numberPad)

                                DatePicker("تاريخ التحميل", selection: $draft.loadDate, displayedComponents: .date)
                                DatePicker("تاريخ التفريغ", selection: $draft.unloadDate, displayedComponents: .date)

                                TextField("المالك (الشركة)", text: $draft.ownerType)
                                    .textFieldStyle(.roundedBorder)

                                Picker("المحطة", selection: $draft.destination) {
                                    ForEach(stations, id: \.self) { Text($0) }
                                }
                                .pickerStyle(.menu)

                                Picker("اسم المعمل", selection: $draft.factoryName) {
                                    Text("اختر المعمل...").tag("")
                                    ForEach(factories, id: \.self) { Text($0) }
                                }
                                .pickerStyle(.menu)

                                TextField("رقم بوچر المعمل", text: $draft.factoryVoucher)
                                    .textFieldStyle(.roundedBorder)

                                TextField("الكمية (طن)", text: $draft.quantity)
                                    .textFieldStyle(.roundedBorder)
                                    .keyboardType(.decimalPad)

                                TextField("لترات الكاز", text: $draft.liters)
                                    .textFieldStyle(.roundedBorder)
                                    .keyboardType(.decimalPad)

                                TextField("السعر (دينار)", text: $draft.price)
                                    .textFieldStyle(.roundedBorder)
                                    .keyboardType(.decimalPad)

                                TextField("ملاحظات", text: $draft.notes, axis: .vertical)
                                    .textFieldStyle(.roundedBorder)
                                    .lineLimit(3...5)

                                PhotosPicker(selection: $selectedPhoto, matching: .images) {
                                    Label(draft.fileData.isEmpty ? "اختيار صورة الوصل" : "تم اختيار صورة الوصل", systemImage: "camera")
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(.bordered)
                            }
                        }

                        Button {
                            Task { await submitTrip() }
                        } label: {
                            HStack {
                                if isSubmitting { ProgressView().tint(.white) }
                                Text("حفظ وإرسال الوصل")
                                    .fontWeight(.bold)
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(AppTheme.orange)
                            .foregroundStyle(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                        }
                        .disabled(isSubmitting)

                        if let resultMessage {
                            SurfaceCard {
                                Text(resultMessage)
                                    .foregroundStyle(resultMessage.contains("نجاح") ? .green : .red)
                            }
                        }
                    }
                    .padding(16)
                }
            }
            .navigationTitle("وصل")
            .task(id: selectedPhoto) {
                await loadPhoto()
            }
        }
    }

    private func loadPhoto() async {
        guard let selectedPhoto,
              let data = try? await selectedPhoto.loadTransferable(type: Data.self) else { return }
        draft.fileData = "data:image/jpeg;base64," + data.base64EncodedString()
    }

    private func submitTrip() async {
        guard !draft.docNumber.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            resultMessage = "يجب إدخال رقم الوصل"
            return
        }

        guard !draft.quantity.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            resultMessage = "يجب إدخال الكمية"
            return
        }

        guard !draft.ownerType.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            resultMessage = "يجب إدخال اسم المالك أو الشركة"
            return
        }

        guard !draft.price.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            resultMessage = "يجب إدخال السعر"
            return
        }

        guard !draft.fileData.isEmpty else {
            resultMessage = "يجب اختيار صورة الوصل"
            return
        }

        isSubmitting = true
        resultMessage = nil

        do {
            if try await APIClient.shared.checkDocNumber(draft.docNumber) {
                resultMessage = "رقم الوصل مسجل مسبقاً"
                isSubmitting = false
                return
            }

            let response = try await APIClient.shared.submitTrip(
                draft: draft,
                driverName: viewModel.session.driverName,
                carNumber: viewModel.session.carNumber
            )

            if response.success {
                resultMessage = "تم إرسال الوصل بنجاح"
                draft = TripSubmissionDraft()
                await viewModel.refreshAll()
            } else {
                resultMessage = response.message ?? "فشل في إرسال الوصل"
            }
        } catch {
            resultMessage = "تعذر إرسال الوصل حالياً"
        }

        isSubmitting = false
    }
}
