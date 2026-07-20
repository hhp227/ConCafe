//
//  CheckInCafeMapView.swift
//  ConCafe
//
//  Created by 홍희표 on 4/8/26.
//

import SwiftUI
import MapKit

struct CheckInCafeMapView: UIViewRepresentable {
    let pins: [CheckInMapPin]

    let cameraRegion: MKCoordinateRegion

    let cameraToken: String

    @Binding var selectedPinId: String?

    let onCafeTap: (String) -> Void

    let onCheckInForCafeTap: (String) -> Void

    var showsCheckInButton: Bool = true

    func makeUIView(context: Context) -> MKMapView {
        let mapView = MKMapView(frame: .zero)
        mapView.delegate = context.coordinator
        mapView.isRotateEnabled = false
        mapView.isPitchEnabled = false
        mapView.showsCompass = false
        return mapView
    }

    func updateUIView(_ mapView: MKMapView, context: Context) {
        context.coordinator.onCafeTap = onCafeTap
        context.coordinator.onCheckInForCafeTap = onCheckInForCafeTap
        context.coordinator.showsCheckInButton = showsCheckInButton
        context.coordinator.syncAnnotations(on: mapView, pins: pins)
        context.coordinator.applySelection(on: mapView, selectedPinId: selectedPinId)
        context.coordinator.applyCamera(
            on: mapView,
            region: cameraRegion,
            token: cameraToken
        )
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(
            selectedPinId: $selectedPinId,
            onCafeTap: onCafeTap,
            onCheckInForCafeTap: onCheckInForCafeTap
        )
    }

    final class Coordinator: NSObject, MKMapViewDelegate {
        private let selectedPinIdBinding: Binding<String?>

        var onCafeTap: (String) -> Void

        var onCheckInForCafeTap: (String) -> Void

        var showsCheckInButton: Bool

        private var annotationsById: [String: CheckInCafeAnnotation] = [:]

        private var lastCameraToken: String?

        private var isApplyingSelection = false

        func syncAnnotations(on mapView: MKMapView, pins: [CheckInMapPin]) {
            let incomingIds = Set(pins.map(\.id))
            let existingIds = Set(annotationsById.keys)

            let removedIds = existingIds.subtracting(incomingIds)
            removedIds.forEach { id in
                if let annotation = annotationsById.removeValue(forKey: id) {
                    mapView.removeAnnotation(annotation)
                }
            }

            pins.forEach { pin in
                if let annotation = annotationsById[pin.id] {
                    annotation.coordinate = CLLocationCoordinate2D(
                        latitude: pin.latitude,
                        longitude: pin.longitude
                    )
                    annotation.title = pin.name
                } else {
                    let annotation = CheckInCafeAnnotation(pin: pin)
                    annotationsById[pin.id] = annotation
                    mapView.addAnnotation(annotation)
                }
            }
        }

        func applySelection(on mapView: MKMapView, selectedPinId: String?) {
            isApplyingSelection = true
            defer { isApplyingSelection = false }

            if let selectedPinId,
               let annotation = annotationsById[selectedPinId] {
                mapView.selectAnnotation(annotation, animated: false)
            } else {
                mapView.selectedAnnotations.forEach { annotation in
                    mapView.deselectAnnotation(annotation, animated: false)
                }
            }
        }

        func applyCamera(on mapView: MKMapView, region: MKCoordinateRegion, token: String) {
            guard lastCameraToken != token else { return }
            lastCameraToken = token
            mapView.setRegion(region, animated: true)
        }

        func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            guard let annotation = annotation as? CheckInCafeAnnotation else { return nil }
            let identifier = "CheckInCafeAnnotationView"
            let view = (mapView.dequeueReusableAnnotationView(withIdentifier: identifier) as? MKMarkerAnnotationView)
                ?? MKMarkerAnnotationView(annotation: annotation, reuseIdentifier: identifier)

            view.annotation = annotation
            view.canShowCallout = true
            view.markerTintColor = UIColor(ConCafeColors.primary)
            view.glyphImage = UIImage(systemName: "cup.and.saucer.fill")
            view.leftCalloutAccessoryView = nil
            view.rightCalloutAccessoryView = nil
            view.detailCalloutAccessoryView = makeCalloutView(for: annotation)
            return view
        }

        func mapView(_ mapView: MKMapView, didSelect annotation: MKAnnotation) {
            guard !isApplyingSelection,
                  let annotation = annotation as? CheckInCafeAnnotation else { return }
            selectedPinIdBinding.wrappedValue = annotation.id
        }

        func mapView(_ mapView: MKMapView, didDeselect annotation: MKAnnotation) {
            guard !isApplyingSelection,
                  let annotation = annotation as? CheckInCafeAnnotation,
                  selectedPinIdBinding.wrappedValue == annotation.id else { return }
            selectedPinIdBinding.wrappedValue = nil
        }

        @objc private func handleCafeButtonTap(_ sender: CheckInCalloutButton) {
            selectedPinIdBinding.wrappedValue = nil
            onCafeTap(sender.cafeId)
        }

        @objc private func handleCheckInButtonTap(_ sender: CheckInCalloutButton) {
            selectedPinIdBinding.wrappedValue = nil
            onCheckInForCafeTap(sender.cafeId)
        }

        private func makeCalloutView(for annotation: CheckInCafeAnnotation) -> UIView {
            let cafeButton = CheckInCalloutButton(type: .system)
            cafeButton.cafeId = annotation.id
            cafeButton.setTitle(annotation.title ?? "", for: .normal)
            cafeButton.setTitleColor(UIColor(ConCafeColors.textPrimary), for: .normal)
            cafeButton.titleLabel?.font = UIFont.systemFont(ofSize: 13, weight: .semibold)
            cafeButton.addTarget(self, action: #selector(handleCafeButtonTap(_:)), for: .touchUpInside)

            let arrangedSubviews: [UIView]
            if showsCheckInButton {
                let checkInButton = CheckInCalloutButton(type: .system)
                checkInButton.cafeId = annotation.id
                checkInButton.setImage(UIImage(systemName: "checkmark.circle.fill"), for: .normal)
                checkInButton.tintColor = UIColor(ConCafeColors.primary)
                checkInButton.addTarget(self, action: #selector(handleCheckInButtonTap(_:)), for: .touchUpInside)
                arrangedSubviews = [cafeButton, checkInButton]
            } else {
                arrangedSubviews = [cafeButton]
            }

            let stackView = UIStackView(arrangedSubviews: arrangedSubviews)
            stackView.axis = .horizontal
            stackView.alignment = .center
            stackView.spacing = 6

            let container = UIView()
            container.addSubview(stackView)
            stackView.translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activate([
                stackView.leadingAnchor.constraint(equalTo: container.leadingAnchor),
                stackView.trailingAnchor.constraint(equalTo: container.trailingAnchor),
                stackView.topAnchor.constraint(equalTo: container.topAnchor),
                stackView.bottomAnchor.constraint(equalTo: container.bottomAnchor)
            ])
            return container
        }

        init(
            selectedPinId: Binding<String?>,
            onCafeTap: @escaping (String) -> Void,
            onCheckInForCafeTap: @escaping (String) -> Void
        ) {
            selectedPinIdBinding = selectedPinId
            self.onCafeTap = onCafeTap
            self.onCheckInForCafeTap = onCheckInForCafeTap
            self.showsCheckInButton = true
        }
    }
}

struct CheckInMapPin: Identifiable {
    let id: String
    let name: String
    let latitude: Double
    let longitude: Double
    let isSelected: Bool
}

private final class CheckInCafeAnnotation: NSObject, MKAnnotation {
    let id: String

    dynamic var coordinate: CLLocationCoordinate2D

    var title: String?

    init(pin: CheckInMapPin) {
        id = pin.id
        coordinate = CLLocationCoordinate2D(latitude: pin.latitude, longitude: pin.longitude)
        title = pin.name
    }
}

private final class CheckInCalloutButton: UIButton {
    var cafeId: String = ""
}
