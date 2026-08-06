# diplomarbeit-gamebased-learning

KI-gestuetzte Spieleplattform fuer Game-based Learning.

## LeoCloud

Dieses Projekt ist fuer ein minimales erstes LeoCloud-Deployment vorbereitet.

- Namespace: `student-it220269`
- Host: `it220269.cloud.htl-leonding.ac.at`
- Image: `ghcr.io/davidberghahn/diplomarbeit-gamebased-learning:latest`

Deployment:

```bash
kubectl config use-context leocloud
kubectl apply -n student-it220269 -f k8s/backend.yaml
kubectl apply -n student-it220269 -f k8s/backend-ingress.yaml
kubectl rollout status deployment/gamebased-learning-web -n student-it220269
```

Danach sollte die Seite erreichbar sein unter:

```text
https://it220269.cloud.htl-leonding.ac.at/
```
