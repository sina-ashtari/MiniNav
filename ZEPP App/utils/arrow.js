// Fixed physical coordinates, never mirrored by language/RTL layout.
export function arrowLines(m) {
  let points = [[100, 135], [100, 30]]
  let head = [[76, 56], [100, 30], [124, 56]]
  const left = m.endsWith('LEFT')
  const side = left || m.endsWith('RIGHT')
  if (side) {
    points = [[115, 135], [115, 60], [40, 60]]
    head = [[68, 32], [40, 60], [68, 88]]
    if (m.startsWith('SLIGHT') || m.startsWith('KEEP') || m.startsWith('EXIT')) {
      points = [[125, 135], [125, 100], [55, 30]]; head = [[55, 68], [55, 30], [93, 30]]
    } else if (m.startsWith('SHARP')) {
      points = [[130, 135], [130, 35], [50, 115]]; head = [[50, 75], [50, 115], [90, 115]]
    } else if (m.startsWith('UTURN')) {
      points = [[140, 135], [140, 40], [120, 20], [70, 20], [50, 40], [50, 105]]
      head = [[25, 80], [50, 105], [75, 80]]
    } else if (m.startsWith('ROUNDABOUT')) {
      points = [[120, 145], [120, 115], [150, 85], [150, 55], [120, 25], [85, 25], [55, 55], [55, 85], [20, 85]]
      head = [[42, 63], [20, 85], [42, 107]]
    }
    if (!left) { points = points.map(([x,y]) => [200-x,y]); head = head.map(([x,y]) => [200-x,y]) }
  }
  if (m === 'MERGE') points = [[55, 135], [100, 85], [100, 30]]
  if (['UNKNOWN', 'ARRIVAL', 'REROUTING'].includes(m)) return []
  return [points, head]
}
