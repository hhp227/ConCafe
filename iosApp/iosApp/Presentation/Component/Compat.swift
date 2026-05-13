//
//  Compat.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/08.
//

import Foundation
import SwiftUI
import UIKit
import PhotosUI
import UniformTypeIdentifiers
import Shared

enum CompatNavigationBarStyle {
    case opaque
    case transparentScrollEdge
}

enum AppBarAppearance {
    static func configureDefaultAppearance() {
        let navigationBarAppearance = makeOpaqueNavigationBarAppearance()
        let navigationBar = UINavigationBar.appearance()
        navigationBar.isTranslucent = false
        navigationBar.standardAppearance = navigationBarAppearance
        navigationBar.scrollEdgeAppearance = navigationBarAppearance
        navigationBar.compactAppearance = navigationBarAppearance
        if #available(iOS 15.0, *) {
            navigationBar.compactScrollEdgeAppearance = navigationBarAppearance
        }

        let tabBarAppearance = makeOpaqueTabBarAppearance()
        let tabBar = UITabBar.appearance()
        tabBar.standardAppearance = tabBarAppearance
        if #available(iOS 15.0, *) {
            tabBar.scrollEdgeAppearance = tabBarAppearance
        }
    }

    static func makeOpaqueNavigationBarAppearance() -> UINavigationBarAppearance {
        let appearance = UINavigationBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor.systemBackground
        appearance.shadowColor = UIColor.separator
        return appearance
    }

    static func makeTransparentNavigationBarAppearance() -> UINavigationBarAppearance {
        let appearance = UINavigationBarAppearance()
        appearance.configureWithTransparentBackground()
        appearance.backgroundColor = .clear
        appearance.shadowColor = .clear
        return appearance
    }

    static func applyNavigationBarStyle(_ style: CompatNavigationBarStyle, to navigationBar: UINavigationBar) {
        applyNavigationBarStyle(style, to: navigationBar, updatesTranslucency: true)
    }

    fileprivate static func applyNavigationBarTransitionStyle(
        _ style: CompatNavigationBarStyle,
        to navigationBar: UINavigationBar
    ) {
        switch style {
        case .opaque:
            applyNavigationBarStyle(.opaque, to: navigationBar)
        case .transparentScrollEdge:
            let transparentAppearance = makeTransparentNavigationBarAppearance()
            navigationBar.isTranslucent = true
            navigationBar.standardAppearance = transparentAppearance
            navigationBar.scrollEdgeAppearance = transparentAppearance
            navigationBar.compactAppearance = transparentAppearance
            if #available(iOS 15.0, *) {
                navigationBar.compactScrollEdgeAppearance = transparentAppearance
            }
        }
    }

    private static func applyNavigationBarStyle(
        _ style: CompatNavigationBarStyle,
        to navigationBar: UINavigationBar,
        updatesTranslucency: Bool
    ) {
        let standardAppearance = makeOpaqueNavigationBarAppearance()
        let scrollEdgeAppearance: UINavigationBarAppearance

        switch style {
        case .opaque:
            if updatesTranslucency {
                navigationBar.isTranslucent = false
            }
            scrollEdgeAppearance = standardAppearance
        case .transparentScrollEdge:
            if updatesTranslucency {
                navigationBar.isTranslucent = true
            }
            scrollEdgeAppearance = makeTransparentNavigationBarAppearance()
        }

        navigationBar.standardAppearance = standardAppearance
        navigationBar.scrollEdgeAppearance = scrollEdgeAppearance
        navigationBar.compactAppearance = standardAppearance
        if #available(iOS 15.0, *) {
            navigationBar.compactScrollEdgeAppearance = scrollEdgeAppearance
        }
    }

    static func applyNavigationBarStyleToVisibleNavigationBars(_ style: CompatNavigationBarStyle) {
        let scenes = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
        let scrollViewOffsets = captureVisibleScrollViewOffsets(in: scenes)

        UIView.performWithoutAnimation {
            scenes
                .flatMap(\.windows)
                .forEach { window in
                    applyNavigationBarTransitionStyle(style, in: window.rootViewController)
                }
            restoreScrollViewOffsets(scrollViewOffsets)
        }

        DispatchQueue.main.async {
            restoreScrollViewOffsets(scrollViewOffsets)
        }
    }

    private static func captureVisibleScrollViewOffsets(in scenes: [UIWindowScene]) -> [(UIScrollView, CGPoint)] {
        scenes
            .flatMap(\.windows)
            .filter { !$0.isHidden }
            .flatMap { collectScrollViewOffsets(in: $0) }
    }

    private static func collectScrollViewOffsets(in view: UIView) -> [(UIScrollView, CGPoint)] {
        var result: [(UIScrollView, CGPoint)] = []

        if let scrollView = view as? UIScrollView {
            result.append((scrollView, scrollView.contentOffset))
        }

        view.subviews.forEach { subview in
            result.append(contentsOf: collectScrollViewOffsets(in: subview))
        }

        return result
    }

    private static func restoreScrollViewOffsets(_ offsets: [(UIScrollView, CGPoint)]) {
        offsets.forEach { scrollView, contentOffset in
            guard scrollView.window != nil else { return }
            scrollView.setContentOffset(contentOffset, animated: false)
        }
    }

    private static func applyNavigationBarStyle(
        _ style: CompatNavigationBarStyle,
        in viewController: UIViewController?
    ) {
        guard let viewController else { return }

        if let navigationController = viewController as? UINavigationController {
            applyNavigationBarStyle(style, to: navigationController.navigationBar)
        }

        viewController.children.forEach {
            applyNavigationBarStyle(style, in: $0)
        }

        applyNavigationBarStyle(style, in: viewController.presentedViewController)
    }

    private static func applyNavigationBarTransitionStyle(
        _ style: CompatNavigationBarStyle,
        in viewController: UIViewController?
    ) {
        guard let viewController else { return }

        if let navigationController = viewController as? UINavigationController {
            applyNavigationBarTransitionStyle(style, to: navigationController.navigationBar)
        }

        viewController.children.forEach {
            applyNavigationBarTransitionStyle(style, in: $0)
        }

        applyNavigationBarTransitionStyle(style, in: viewController.presentedViewController)
    }

    fileprivate static func updateScrollContentInsetAdjustmentBehavior(
        from view: UIView,
        behavior: UIScrollView.ContentInsetAdjustmentBehavior
    ) {
        var currentView = view.superview

        while let unwrappedView = currentView {
            if let scrollView = unwrappedView as? UIScrollView {
                scrollView.contentInsetAdjustmentBehavior = behavior
                scrollView.contentInset.top = 0
                scrollView.scrollIndicatorInsets.top = 0
                break
            } else {
                currentView = unwrappedView.superview
            }
        }
    }

    private static func makeOpaqueTabBarAppearance() -> UITabBarAppearance {
        let appearance = UITabBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor.systemBackground
        appearance.shadowColor = UIColor.separator
        return appearance
    }

}

struct ExploreKeyboardDismissModifier: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) {
            content.scrollDismissesKeyboard(.immediately)
        } else {
            content
        }
    }
}

struct ScrollViewKeyboardDismissConfigurator: UIViewRepresentable {
    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: .zero)

        DispatchQueue.main.async {
            updateKeyboardDismissMode(from: view)
        }

        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        DispatchQueue.main.async {
            updateKeyboardDismissMode(from: uiView)
        }
    }

    private func updateKeyboardDismissMode(from view: UIView) {
        var currentView: UIView? = view.superview

        while let unwrappedView = currentView {
            if let scrollView = unwrappedView as? UIScrollView {
                scrollView.keyboardDismissMode = .onDrag
                break
            } else {
                currentView = unwrappedView.superview
            }
        }
    }
}

struct ScrollViewContentInsetAdjustmentConfigurator: UIViewRepresentable {
    let behavior: UIScrollView.ContentInsetAdjustmentBehavior

    func makeUIView(context: Context) -> ContentInsetAdjustmentView {
        ContentInsetAdjustmentView(behavior: behavior)
    }

    func updateUIView(_ uiView: ContentInsetAdjustmentView, context: Context) {
        uiView.behavior = behavior
        uiView.updateContentInsetAdjustmentBehavior()
    }

    final class ContentInsetAdjustmentView: UIView {
        var behavior: UIScrollView.ContentInsetAdjustmentBehavior {
            didSet {
                updateContentInsetAdjustmentBehavior()
            }
        }

        init(behavior: UIScrollView.ContentInsetAdjustmentBehavior) {
            self.behavior = behavior
            super.init(frame: .zero)
            isHidden = true
            isUserInteractionEnabled = false
        }

        @available(*, unavailable)
        required init?(coder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }

        override func didMoveToSuperview() {
            super.didMoveToSuperview()
            updateContentInsetAdjustmentBehavior()
        }

        override func didMoveToWindow() {
            super.didMoveToWindow()
            updateContentInsetAdjustmentBehavior()
        }

        override func layoutSubviews() {
            super.layoutSubviews()
            updateContentInsetAdjustmentBehavior()
        }

        func updateContentInsetAdjustmentBehavior() {
            AppBarAppearance.updateScrollContentInsetAdjustmentBehavior(from: self, behavior: behavior)
        }
    }
}

extension View {
    @ViewBuilder
    func compatScrollTargetLayout() -> some View {
        if #available(iOS 17.0, *) {
            self.scrollTargetLayout()
        } else {
            self
        }
    }

    @ViewBuilder
    func compatViewAlignedScrollSnap() -> some View {
        if #available(iOS 17.0, *) {
            self.scrollTargetBehavior(.viewAligned)
        } else {
            self
        }
    }
}

struct CompatNavigationContainer<Content: View>: View {
    private let title: String?

    private let content: Content

    var body: some View {
        if #available(iOS 16.0, *) {
            NavigationStack {
                wrappedContent
            }
        } else {
            NavigationView {
                wrappedContent
            }
            .navigationViewStyle(StackNavigationViewStyle())
        }
    }

    @ViewBuilder
    private var wrappedContent: some View {
        if let title {
            content
                .navigationTitle(title)
                .navigationBarTitleDisplayMode(.inline)
        } else {
            content
        }
    }

    init(
        title: String? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.content = content()
    }
}

struct CompatPresentationBackgroundModifier: ViewModifier {
    let color: Color

    func body(content: Content) -> some View {
        if #available(iOS 16.4, *) {
            content.presentationBackground(color)
        } else {
            content
        }
    }
}

struct CompatLargeSheetDetentModifier: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) {
            content.presentationDetents([.large])
        } else {
            content
        }
    }
}

struct CompatMediumSheetDetentModifier: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) {
            content.presentationDetents([.medium])
        } else {
            content
        }
    }
}

struct CompatFractionSheetDetentModifier: ViewModifier {
    let fraction: CGFloat

    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) {
            content.presentationDetents([.fraction(fraction)])
        } else {
            content
        }
    }
}

struct CompatPresentationDragIndicatorModifier: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) {
            content.presentationDragIndicator(.visible)
        } else {
            content
        }
    }
}

struct CompatSafeAreaBottomPaddingModifier: ViewModifier {
    func body(content: Content) -> some View {
        content.padding(.bottom, Self.bottomSafeAreaInset)
    }

    private static var bottomSafeAreaInset: CGFloat {
        let scenes = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
        let keyWindow = scenes
            .flatMap(\.windows)
            .first { $0.isKeyWindow }
        return keyWindow?.safeAreaInsets.bottom ?? 0
    }
}

struct CompatVerticalTextField: View {
    let placeholder: String

    @Binding var text: String

    var body: some View {
        if #available(iOS 16.0, *) {
            TextField(placeholder, text: $text, axis: .vertical)
                .lineLimit(1...3)
        } else {
            TextField(placeholder, text: $text)
                .lineLimit(1)
        }
    }
}

final class NavigationBarAppearanceHostingController: UIViewController {
    private static var transitionStyle: CompatNavigationBarStyle?

    var style: CompatNavigationBarStyle = .opaque

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        applyAppearanceIfNeeded()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        applyAppearanceIfNeeded()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if let transitionStyle = Self.transitionStyle {
            if let navigationBar = navigationController?.navigationBar {
                AppBarAppearance.applyNavigationBarTransitionStyle(transitionStyle, to: navigationBar)
            }
            Self.transitionStyle = nil
        } else {
            restoreOpaqueAppearance()
        }
    }

    func applyAppearanceIfNeeded() {
        guard let navigationBar = navigationController?.navigationBar else { return }

        AppBarAppearance.applyNavigationBarStyle(style, to: navigationBar)
    }

    private func restoreOpaqueAppearance() {
        guard let navigationBar = navigationController?.navigationBar else { return }

        let opaqueAppearance = AppBarAppearance.makeOpaqueNavigationBarAppearance()
        navigationBar.isTranslucent = false
        navigationBar.standardAppearance = opaqueAppearance
        navigationBar.scrollEdgeAppearance = opaqueAppearance
        navigationBar.compactAppearance = opaqueAppearance
        if #available(iOS 15.0, *) {
            navigationBar.compactScrollEdgeAppearance = opaqueAppearance
        }
    }

    static func prepareTransition(to style: CompatNavigationBarStyle) {
        transitionStyle = style
        AppBarAppearance.applyNavigationBarStyleToVisibleNavigationBars(style)
    }
}

struct NavigationBarAppearanceConfigurator: UIViewControllerRepresentable {
    let style: CompatNavigationBarStyle

    func makeUIViewController(context: Context) -> NavigationBarAppearanceHostingController {
        let controller = NavigationBarAppearanceHostingController()
        controller.view.isHidden = true
        return controller
    }

    func updateUIViewController(_ uiViewController: NavigationBarAppearanceHostingController, context: Context) {
        uiViewController.style = style
        DispatchQueue.main.async {
            uiViewController.applyAppearanceIfNeeded()
        }
    }
}

final class NavigationBarVisibilityHostingController: UIViewController {
    var hideOnDisappear: Bool = false

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(false, animated: animated)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)

        guard hideOnDisappear else { return }
        navigationController?.setNavigationBarHidden(true, animated: animated)
    }
}

struct NavigationBarVisibilityConfigurator: UIViewControllerRepresentable {
    let hideOnDisappear: Bool

    func makeUIViewController(context: Context) -> NavigationBarVisibilityHostingController {
        let controller = NavigationBarVisibilityHostingController()
        controller.view.isHidden = true
        return controller
    }

    func updateUIViewController(_ uiViewController: NavigationBarVisibilityHostingController, context: Context) {
        uiViewController.hideOnDisappear = hideOnDisappear
    }
}

extension View {
    @ViewBuilder
    func compatMapNavigationBarAppearance() -> some View {
        if #available(iOS 16.0, *) {
            self
                .toolbarBackground(.hidden, for: .navigationBar)
                .toolbarColorScheme(.dark, for: .navigationBar)
        } else {
            self.background(
                NavigationBarAppearanceConfigurator(style: .transparentScrollEdge)
                    .frame(width: 0, height: 0)
            )
        }
    }

    @ViewBuilder
    func compatSearchSuggestions(
        cafes: [CheckInCafeSummary],
        onSelect: @escaping (String) -> Void
    ) -> some View {
        if #available(iOS 16.0, *) {
            self.searchSuggestions {
                ForEach(cafes, id: \.id) { cafe in
                    Button(cafe.name) {
                        onSelect(cafe.name)
                    }
                }
            }
        } else {
            self
        }
    }
}

extension View {
    func compatPresentationBackground(_ color: Color) -> some View {
        modifier(CompatPresentationBackgroundModifier(color: color))
    }

    func compatLargeSheetDetent() -> some View {
        modifier(CompatLargeSheetDetentModifier())
    }

    func compatMediumSheetDetent() -> some View {
        modifier(CompatMediumSheetDetentModifier())
    }

    func compatFractionSheetDetent(_ fraction: CGFloat) -> some View {
        modifier(CompatFractionSheetDetentModifier(fraction: fraction))
    }

    func compatPresentationDragIndicator() -> some View {
        modifier(CompatPresentationDragIndicatorModifier())
    }

    func compatSafeAreaBottomPadding() -> some View {
        modifier(CompatSafeAreaBottomPaddingModifier())
    }

    func compatNavigationBarStyle(_ style: CompatNavigationBarStyle) -> some View {
        background(NavigationBarAppearanceConfigurator(style: style))
    }

    @ViewBuilder
    func compatNavigationBarHidden(_ hidden: Bool) -> some View {
        if #available(iOS 16.0, *) {
            toolbar(hidden ? .hidden : .visible, for: .navigationBar)
        } else {
            navigationBarHidden(hidden)
        }
    }

    func compatNavigationBarTransition(hideOnDisappear: Bool) -> some View {
        background(NavigationBarVisibilityConfigurator(hideOnDisappear: hideOnDisappear))
    }

    func compatScrollContentInsetAdjustmentNever() -> some View {
        background(ScrollViewContentInsetAdjustmentConfigurator(behavior: .never))
    }
}

func compatSystemImageName(iOS16: String, fallback: String) -> String {
    if #available(iOS 16.0, *) {
        return iOS16
    } else {
        return fallback
    }
}

struct CompatImagePicker: View {
    let onImageSelected: (UIImage) -> Void

    let onDismiss: () -> Void

    var body: some View {
        // Use one stable picker path across iOS 15/16 to avoid callback-loss regressions.
        PHPickerCompatImagePicker(
            onImageSelected: onImageSelected,
            onDismiss: onDismiss
        )
    }
}

private struct PHPickerCompatImagePicker: UIViewControllerRepresentable {
    let onImageSelected: (UIImage) -> Void

    let onDismiss: () -> Void

    func makeUIViewController(context: Context) -> PHPickerViewController {
        var configuration = PHPickerConfiguration(photoLibrary: PHPhotoLibrary.shared())
        configuration.selectionLimit = 1
        configuration.filter = .images
        let picker = PHPickerViewController(configuration: configuration)
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: PHPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    final class Coordinator: NSObject, PHPickerViewControllerDelegate {
        private let parent: PHPickerCompatImagePicker

        func pickerDidCancel(_ picker: PHPickerViewController) {
            picker.dismiss(animated: true)
            parent.onDismiss()
        }

        func picker(
            _ picker: PHPickerViewController,
            didFinishPicking results: [PHPickerResult]
        ) {
            guard let provider = results.first?.itemProvider else {
                DispatchQueue.main.async {
                    picker.dismiss(animated: true)
                    self.parent.onDismiss()
                }
                return
            }
            loadImage(from: provider) { image in
                DispatchQueue.main.async {
                    if let image {
                        self.parent.onImageSelected(image)
                    }
                    picker.dismiss(animated: true)
                    self.parent.onDismiss()
                }
            }
        }

        private func loadImage(
            from provider: NSItemProvider,
            completion: @escaping (UIImage?) -> Void
        ) {
            if provider.canLoadObject(ofClass: UIImage.self) {
                provider.loadObject(ofClass: UIImage.self) { object, _ in
                    if let image = object as? UIImage {
                        completion(image)
                    } else {
                        self.loadImageFromDataRepresentation(from: provider, completion: completion)
                    }
                }
                return
            }
            loadImageFromDataRepresentation(from: provider, completion: completion)
        }

        private func loadImageFromDataRepresentation(
            from provider: NSItemProvider,
            completion: @escaping (UIImage?) -> Void
        ) {
            let imageTypeIdentifier = UTType.image.identifier
            guard provider.hasItemConformingToTypeIdentifier(imageTypeIdentifier) else {
                completion(nil)
                return
            }
            provider.loadDataRepresentation(forTypeIdentifier: imageTypeIdentifier) { data, _ in
                guard let data, let image = UIImage(data: data) else {
                    completion(nil)
                    return
                }
                completion(image)
            }
        }

        init(parent: PHPickerCompatImagePicker) {
            self.parent = parent
        }
    }
}

@available(iOS 16.0, *)
private struct PhotosUICompatImagePicker: View {
    let onImageSelected: (UIImage) -> Void

    let onDismiss: () -> Void

    @State private var selectedItem: PhotosPickerItem?

    @State private var isLoading = false

    var body: some View {
        NavigationView {
            VStack(spacing: 18) {
                Text("이미지 선택")
                    .font(.headline)
                    .foregroundStyle(.secondary)
                    .padding(.top, 20)
                PhotosPicker(
                    selection: $selectedItem,
                    matching: .images
                ) {
                    Label("앨범에서 사진 선택", systemImage: "photo.on.rectangle.angled")
                        .padding(.horizontal, 18)
                        .padding(.vertical, 12)
                        .background(.blue.opacity(0.18))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .disabled(isLoading)
                if isLoading {
                    ProgressView("이미지 로딩 중")
                }
                Spacer()
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 16)
            .navigationTitle("항목 이미지 선택")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("취소", action: onDismiss)
                }
            }
        }
        .onChange(of: selectedItem) { item in
            guard let item else { return }

            isLoading = true
            Task {
                defer {
                    Task { @MainActor in
                        isLoading = false
                        // Allow selecting the same or another asset again on subsequent picker opens.
                        selectedItem = nil
                    }
                }
                if let data = try? await item.loadTransferable(type: Data.self),
                   let image = UIImage(data: data) {
                    await MainActor.run {
                        onImageSelected(image)
                        onDismiss()
                    }
                } else {
                    await MainActor.run {
                        onDismiss()
                    }
                }
            }
        }
    }
}

private struct RoundedCornerShape: Shape {
    var topLeft: CGFloat = 0
    
    var topRight: CGFloat = 0
    
    var bottomLeft: CGFloat = 0
    
    var bottomRight: CGFloat = 0

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath()
        let tl = min(min(topLeft, rect.width / 2), rect.height / 2)
        let tr = min(min(topRight, rect.width / 2), rect.height / 2)
        let bl = min(min(bottomLeft, rect.width / 2), rect.height / 2)
        let br = min(min(bottomRight, rect.width / 2), rect.height / 2)

        path.move(to: CGPoint(x: rect.minX + tl, y: rect.minY))

        // top
        path.addLine(to: CGPoint(x: rect.maxX - tr, y: rect.minY))
        path.addArc(withCenter: CGPoint(x: rect.maxX - tr, y: rect.minY + tr),
                    radius: tr,
                    startAngle: -.pi / 2,
                    endAngle: 0,
                    clockwise: true)

        // right
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY - br))
        path.addArc(withCenter: CGPoint(x: rect.maxX - br, y: rect.maxY - br),
                    radius: br,
                    startAngle: 0,
                    endAngle: .pi / 2,
                    clockwise: true)

        // bottom
        path.addLine(to: CGPoint(x: rect.minX + bl, y: rect.maxY))
        path.addArc(withCenter: CGPoint(x: rect.minX + bl, y: rect.maxY - bl),
                    radius: bl,
                    startAngle: .pi / 2,
                    endAngle: .pi,
                    clockwise: true)

        // left
        path.addLine(to: CGPoint(x: rect.minX, y: rect.minY + tl))
        path.addArc(withCenter: CGPoint(x: rect.minX + tl, y: rect.minY + tl),
                    radius: tl,
                    startAngle: .pi,
                    endAngle: 3 * .pi / 2,
                    clockwise: true)

        path.close()
        return Path(path.cgPath)
    }
}

extension View {

    /// iOS 15 compatible corner radius (각 코너별 지정 가능)
    func cornerRadius(
        topLeft: CGFloat = 0,
        topRight: CGFloat = 0,
        bottomLeft: CGFloat = 0,
        bottomRight: CGFloat = 0
    ) -> some View {
        clipShape(
            RoundedCornerShape(
                topLeft: topLeft,
                topRight: topRight,
                bottomLeft: bottomLeft,
                bottomRight: bottomRight
            )
        )
    }

    /// iOS 16 이상이면 native API 사용, 아니면 fallback
    @ViewBuilder
    func cornerRadiusCompat(
        topLeft: CGFloat = 0,
        topRight: CGFloat = 0,
        bottomLeft: CGFloat = 0,
        bottomRight: CGFloat = 0
    ) -> some View {
        if #available(iOS 16.0, *) {
            self.clipShape(
                .rect(
                    topLeadingRadius: topLeft,
                    bottomLeadingRadius: bottomLeft,
                    bottomTrailingRadius: bottomRight,
                    topTrailingRadius: topRight
                )
            )
        } else {
            self.clipShape(
                RoundedCornerShape(
                    topLeft: topLeft,
                    topRight: topRight,
                    bottomLeft: bottomLeft,
                    bottomRight: bottomRight
                )
            )
        }
    }
}
