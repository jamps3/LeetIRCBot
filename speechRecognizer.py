import speech_recognition as sr

# Initialize recognizer class (for recognizing the speech)
r = sr.Recognizer()

# Function that will be called everytime audio is captured
def callback(recognizer, audio):
    try:
        # Listen to the audio data
        text = recognizer.recognize_google(audio)
        # check if the word 'krak' is present in the transcribed text
        if 'krak' in text:
            print("The word 'krak' was spoken!")
    except sr.UnknownValueError:
        print("Could not understand audio")
    except sr.RequestError as e:
        print("Could not request results; {0}".format(e))

# start listening to microphone 
with sr.Microphone() as source:
    r.adjust_for_ambient_noise(source)
    r.energy_threshold = 2000
    r.dynamic_energy_threshold = True
    r.listen_in_background(source, callback)
    input("Press Enter to stop listening...")
