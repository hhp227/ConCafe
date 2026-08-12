import Foundation
import Shared

enum DormantAccountAction {
    case selectFilter(DormantAccountFilter)
    case loadMore
    case requestDormantChange(User, Bool)
    case confirmDormantChange
    case cancelDormantChange
    case dismissInfoMessage
}
