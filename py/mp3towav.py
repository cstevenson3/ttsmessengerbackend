import sys
from pydub import AudioSegment

#argv = path, text

path = False
for arg in sys.argv:
    if arg.endswith(".mp3"):
        path = arg

if path is False:
    print("Input file arg required")
    sys.exit()

sound = AudioSegment.from_mp3(path)
splitted = path.split(".")
resultPath = ""
for part in splitted[0:-1]:
    resultPath += part + "."
print(resultPath)
sound.export(resultPath + "wav", format="wav")