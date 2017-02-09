#include "qmapbox.hpp"

#include <mbgl/gl/extension.hpp>
#include <mbgl/map/change.hpp>
#include <mbgl/storage/network_status.hpp>
#include <mbgl/util/default_styles.hpp>
#include <mbgl/util/geometry.hpp>
#include <mbgl/util/traits.hpp>

#if QT_VERSION >= 0x050000
#include <QOpenGLContext>
#else
#include <QGLContext>
#endif

// mbgl::NetworkStatus::Status
static_assert(mbgl::underlying_type(QMapbox::Online) == mbgl::underlying_type(mbgl::NetworkStatus::Status::Online), "error");
static_assert(mbgl::underlying_type(QMapbox::Offline) == mbgl::underlying_type(mbgl::NetworkStatus::Status::Offline), "error");

// mbgl::FeatureType
static_assert(mbgl::underlying_type(QMapbox::Feature::PointType) == mbgl::underlying_type(mbgl::FeatureType::Point), "error");
static_assert(mbgl::underlying_type(QMapbox::Feature::LineStringType) == mbgl::underlying_type(mbgl::FeatureType::LineString), "error");
static_assert(mbgl::underlying_type(QMapbox::Feature::PolygonType) == mbgl::underlying_type(mbgl::FeatureType::Polygon), "error");

namespace QMapbox {

/*!
    \namespace QMapbox
    \inmodule Mapbox Qt SDK

    Contains miscellaneous Mapbox bindings used throughout QMapboxGL.
*/

/*!
    \typedef QMapbox::Coordinate

    Synonim for QPair<double, double>.
    Reflects mbgl::Point<double>.
*/

/*!
    \typedef QMapbox::CoordinateZoom

    Synonim for QPair<Coordinate, double>.
    Used as return value in QMapboxGL::coordinateZoomForBounds.
*/

/*!
    \typedef QMapbox::Coordinates

    Synonim for QList<QMapbox::Coordinate>.
    Reflects mbgl::{LineString,LinearRing,MultiPoint}<double>.
*/

/*!
    \typedef QMapbox::CoordinatesCollection

    Synonim for QList<QMapbox::Coordinates>.
    Reflects mbgl::{MultiLineString,Polygon}<double>.
*/

/*!
    \typedef QMapbox::CoordinatesCollections

    Synonim for QList<QMApbox::CoordinatesCollection>.
    Reflects mbgl::MultiPolygon<double>.
*/

/*!
    \class QMapbox::Feature
    \brief Reflects mbgl::Feature.

    \inmodule Mapbox Qt SDK

    QMapbox::Feature is a struct representing mbgl::Feature data contents.

    Features are represented via its \a type (PointType, LineStringType or PolygonType), \a geometry, \a properties map and \a id (optional).
*/

/*!
    \enum QMapbox::Feature::Type

    This enum is used as basis for geometry disambiguation in QMapbox::Feature.

    Reflects mbgl::FeatureType.

    \value PointType      A point geometry type. Means a single or a collection of points.
    \value LineStringType A line string geometry type. Means a single or a collection of line strings.
    \value PolygonType    A polygon geometry type. Means a single or a collection of polygons.
*/

/*!
    \class QMapbox::ShapeAnnotationGeometry
    \brief Reflects mbgl::ShapeAnnotationGeometry.

    \inmodule Mapbox Qt SDK

    QMapbox::ShapeAnnotationGeometry is a struct representing a shape annotation geometry, reflecting mbgl::ShapeAnnotationGeometry contents.
*/

/*!
    \enum QMapbox::ShapeAnnotationGeometry::Type

    This enum is used as basis for geometry disambiguation in QMapbox::ShapeAnnotationGeometry.

    \value PolygonType         A polygon geometry type.
    \value LineStringType      A line string geometry type.
    \value MultiPolygonType    A polygon geometry collection type.
    \value MultiLineStringType A line string geometry collection type.
*/

/*!
    \class QMapbox::SymbolAnnotation
    \brief Reflects mbgl::SymbolAnnotation.

    \inmodule Mapbox Qt SDK

    QMapbox::SymbolAnnotation is a struct representing a symbol annotation, reflecting mbgl::SymbolAnnotation contents.
*/

/*!
    \class QMapbox::LineAnnotation
    \brief Reflects mbgl::LineAnnotation.

    \inmodule Mapbox Qt SDK

    QMapbox::LineAnnotation is a struct representing a line annotation, reflecting mbgl::LineAnnotation contents.
*/

/*!
    \class QMapbox::FillAnnotation
    \brief Reflects mbgl::FillAnnotation.

    \inmodule Mapbox Qt SDK

    QMapbox::FillAnnotation is a struct representing a fill annotation, reflecting mbgl::FillAnnotation contents.
*/

/*!
    \class QMapbox::StyleSourcedAnnotation
    \brief Reflects mbgl::StyleSourcedAnnotation.

    \inmodule Mapbox Qt SDK

    QMapbox::StyleSourcedAnnotation is a struct representing a style sourced annotation, reflecting mbgl::StyleSourcedAnnotation contents.
*/

/*!
    \typedef QMapbox::Annotation

    Synonim for a QVariant that encapsulates either a symbol, a line, a fill or a style sourced annotation.
    Reflects mbgl::Annotation.
*/

/*!
    \typedef QMapbox::AnnotationID

    Synonim for quint32 representing an annotation identifier.
    Reflects mbgl::AnnotationID.
*/

/*!
    \typedef QMapbox::AnnotationIDs

    Synonim for QList<quint32> representing a container of annotation identifiers.
    Reflects mbgl::AnnotationIDs.
*/

/*!
    \typedef QMapbox::CustomLayerDeinitializeFunction

    Represents a callback to be called when deinitializing a custom layer.

    Reflects mbgl::style::tializeFunction.
*/

/*!
    \typedef QMapbox::CustomLayerInitializeFunction

    Represents a callback to be called when initializing a custom layer.

    Reflects mbgl::style::CustomLayerInitializeFunction.
*/

/*!
    \typedef QMapbox::CustomLayerRenderFunction

    Represents a callback to be called on each render pass for a custom layer.

    Reflects mbgl::style::CustomLayerRenderFunction.
*/

/*!
    \enum QMapbox::NetworkMode
    \brief Reflects mbgl::util::NetworkStatus::Status.

    This enum represents whether server requests can be performed via network.

    \value Online  Server network requests are accessible.
    \value Offline Only requests to the local cache are accessible.
*/

/*!
    \class QMapbox::CustomLayerRenderParameters
    \inmodule Mapbox Qt SDK

    QMapbox::CustomLayerRenderParameters is a struct that represents the data passed on each render pass for a custom layer.
    Reflects mbgl::CustomLayerRenderParameters.
*/

/*!
    \fn QMapbox::NetworkMode QMapbox::networkMode()

    Returns the current QMapbox::NetworkMode.
*/
Q_DECL_EXPORT NetworkMode networkMode()
{
    return static_cast<NetworkMode>(mbgl::NetworkStatus::Get());
}

/*!
    \fn void QMapbox::setNetworkMode(QMapbox::NetworkMode mode)

    Forwards the network status \a mode to Mapbox GL Native engine.
*/
Q_DECL_EXPORT void setNetworkMode(NetworkMode mode)
{
    mbgl::NetworkStatus::Set(static_cast<mbgl::NetworkStatus::Status>(mode));
}

/*!
    \fn QList<QPair<QString, QString> >& QMapbox::defaultStyles()

    Returns a QList containing a QPair of QString objects, representing the style URL and name, respectively.
*/
Q_DECL_EXPORT QList<QPair<QString, QString>>& defaultStyles()
{
    static QList<QPair<QString, QString>> styles;

    if (styles.isEmpty()) {
        for (auto style : mbgl::util::default_styles::orderedStyles) {
            styles.append(QPair<QString, QString>(
                QString::fromStdString(style.url), QString::fromStdString(style.name)));
        }
    }

    return styles;
}

/*!
    \fn void QMapbox::initializeGLExtensions()

    Initializes the OpenGL extensions required by Mapbox GL Native engine.
    Must be called once, after an OpenGL context is available.
*/
Q_DECL_EXPORT void initializeGLExtensions()
{
    mbgl::gl::InitializeExtensions([](const char* name) {
#if QT_VERSION >= 0x050000
        QOpenGLContext* thisContext = QOpenGLContext::currentContext();
        return thisContext->getProcAddress(name);
#else
        const QGLContext* thisContext = QGLContext::currentContext();
        return reinterpret_cast<mbgl::gl::glProc>(thisContext->getProcAddress(name));
#endif
    });
}

} // namespace QMapbox
