import { onBeforeUnmount, onMounted, ref } from 'vue'
export function useMediaQuery(query:string) {
  const matches=ref(false)
  let media:MediaQueryList|undefined
  const update=()=>{ matches.value=media?.matches ?? false }
  onMounted(()=>{ media=window.matchMedia(query); update(); media.addEventListener('change',update) })
  onBeforeUnmount(()=>media?.removeEventListener('change',update))
  return matches
}
